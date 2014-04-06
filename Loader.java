import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;


public class Loader {

	
    public static String dbName = "test";
    public static String collectionName = "insert_test";
    public static int batchSize;
    public static String serverName = "localhost";
    public static int serverPort = 27017;
    
    private static int numInserts = 0;
    
    public static Integer numMaxInserts = 16000000;
    public static int documentsPerInsert;


    public static String genString(java.util.Random rand, String thisMask) {
        String returnString = "";
        for (int i = 0, n = thisMask.length() ; i < n ; i++) { 
            char c = thisMask.charAt(i); 
            if (c == '#') {
                returnString += String.valueOf(rand.nextInt(10));
            } else if (c == '@') {
                returnString += (char) (rand.nextInt(26) + 'a');
            } else {
                returnString += c;
            }
        }
        return returnString;
    }


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			documentsPerInsert = Integer.valueOf(args[0]);
			
	        WriteConcern myWC = new WriteConcern();
	        myWC = WriteConcern.SAFE;
	
	        MongoClientOptions clientOptions = new MongoClientOptions.Builder().connectionsPerHost(2048).socketTimeout(60000).writeConcern(myWC).build();
	        ServerAddress srvrAdd = new ServerAddress(serverName,serverPort);
	        MongoClient m = new MongoClient(srvrAdd, clientOptions);
	
	        DB db = m.getDB(dbName);
	        
	        
            DBCollection coll = db.getCollection(collectionName);
            
            coll.drop();
            coll.ensureIndex(new BasicDBObject("k", 1));
            
            java.util.Random rand = new java.util.Random((long) 1);
	        
            // Pre-create the objects
            BasicDBObject[] aDocs = new BasicDBObject[documentsPerInsert];
            for (int i=0; i < documentsPerInsert; i++) {
            	BasicDBObject doc = new BasicDBObject();
                String cVal = genString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
                doc.put("c",cVal);
                String padVal = genString(rand, "###########-###########-###########-###########-###########");
                doc.put("pad",padVal);
            	aDocs[i] = doc;
            }
            
            int reportingRounds = numMaxInserts / 100000;
            int numRounds = (numMaxInserts / documentsPerInsert) / reportingRounds;
            
            BasicDBObject doc = null;
            
            int id = 0;
                        
            long start = System.currentTimeMillis();
            long runningTime = 0;
            
            for (int reportingRoundNum = 0; reportingRoundNum < reportingRounds; reportingRoundNum++) {

            	long batchStart = System.currentTimeMillis();
            	long startInserts = numInserts;
            
	            for (int roundNum = 0; roundNum < numRounds; roundNum++) {
	            	
	                for (int i = 0; i < documentsPerInsert; i++) {
	                    id++;
	                    doc = aDocs[i];
	                    doc.put("_id",id);
	                    doc.put("k",rand.nextInt(numMaxInserts)+1);
	                }
	                
	                coll.insert(aDocs);
	                
	
	                numInserts += documentsPerInsert;
	            }

	            long batchEnd = System.currentTimeMillis();
                long dur = batchEnd - batchStart;
                runningTime+= dur;
                long inserted = numInserts-startInserts;
                double ips = (inserted / (double)Math.max(1, dur))*1000;
                System.out.println(numInserts + "\t" + dur/1000  + "\t" + runningTime/1000 + "\t" + ips);

            }

            long end = System.currentTimeMillis();
            long dur = end - start;
            double ips = (numInserts / (double)Math.max(1, dur))*1000;
            System.out.println(numInserts + "\t" + dur/1000 + "\t" + ips);

		}
		catch (Exception e) {
			System.out.println("oopss: " + e);
		}
	}

}

