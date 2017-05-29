package ch.unibe.scg.methodnullabilityplugin.eea;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the CSV data as a list of {@link NullabilityRecord} from a specified file. 
 * 
 * <p>The raw CSV data is grouped according to groupId, artifactId, class and method.</p>
 *
 * NB: Uses H2 In-Memory Database to access CSV nullability data. 
 * Cf. http://www.javatips.net/blog/h2-in-memory-database-example". 
 */
public class CsvAccessor {

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

    public static List<NullabilityRecord> loadCsv(String filename) {
        try {
        	List<NullabilityRecord> result = new ArrayList<>();
        	try (Connection connection = getDBConnection()) {
        		Statement statement = connection.createStatement();
	            String groupQuery = "SELECT groupId, artifactId, class, method, sum(CONVERT(checks, INT)) as numChecks, sum(CONVERT(invocations, INT)) as numInvocations "
	            		+ "from CSVREAD('" + filename + "') "
	            		+ "group by groupId, artifactId, class, method order by groupId, artifactId, class, method;";
	            ResultSet resultSet = statement.executeQuery(groupQuery);
	            while (resultSet.next()) {
	                String g = resultSet.getString("groupId");
					String a = resultSet.getString("artifactId");
					String c = resultSet.getString("class");
					String m = resultSet.getString("method");
					int nc = resultSet.getInt("numChecks");
					int ni = resultSet.getInt("numInvocations");
	                NullabilityRecord e = new NullabilityRecord(g, a, c, m, nc, ni);
	                result.add(e);
	            }
        	}
            
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    	
    private static Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
    
    public static class NullabilityRecord {
		// "groupId","artifactId","version","class","method","checks","invocations"
		private String groupId;
		private String artifactId;
		private String clazz;
		private String method;
		public final int checks;
		public final int invocations;

		NullabilityRecord(String g, String a, String c, String m, int cs, int i) {
			this.groupId = g;
			this.artifactId = a;
			this.clazz = c;
			this.method = m;
			this.checks = cs;
			this.invocations = i;
		}

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public String getClazz() {
			return clazz;
		}

		public String getMethod() {
			return method;
		}

		public int getChecks() {
			return checks;
		}

		public int getInvocations() {
			return invocations;
		}
		
		public boolean hasInvocations() {
			return invocations > 0;
		}
		
		public double nullability() {
			return (double) checks / invocations;
		}

		@Override
		public String toString() {
			return "NullabilityRecord [groupId=" + groupId + ", artifactId=" + artifactId + ", clazz=" + clazz
					+ ", method=" + method + ", checks=" + checks + ", invocations=" + invocations + "]";
		}
	}
    
    
}