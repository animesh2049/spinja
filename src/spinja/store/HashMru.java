package spinja.store;


//import com.mongodb.BasicDBObject;
//import com.mongodb.MongoClient;
import com.mongodb.client.model.*;
import static com.mongodb.client.model.Filters.*;
//import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import com.mongodb.async.client.*;
import com.mongodb.async.*;

import java.util.HashSet;

/**
 * Created by harry7 on 3/8/16.
 */
public class HashMru extends StateStore {
    static Node head;
    Node tail;
    HashSet<String> hs;
    JDBCWrapper wrapper;
    int to_swap;
    int totalStoredState;
    long max_mem;
    long hash_hits;
    long disk_hits;
    long disk_inserts;
    long disk_searches;

    public HashMru() {
        head = null;
        tail = null;
        hs = new HashSet();
        wrapper = new JDBCWrapper("localhost", 7777, "test", "spinja");
        totalStoredState = 0;
        to_swap = 0;
        Runtime inst = Runtime.getRuntime();
        max_mem = inst.maxMemory();
        System.err.println("Max Memory used: "+max_mem/(1024*1024));
        hash_hits = 0;
        disk_hits = 0;
        disk_inserts = 0;
        disk_searches = 0;
    }

    public String byteToString(byte[] b)
    {
        String res = "";
        int l = b.length;
        for (int i=0; i<l; i++)
            res += b[i];
        return res;
    }

    public int addState(byte[] state) {
        String key = byteToString(state);
        int ret = 1;
        long mb = 1024 * 1024;
        if (to_swap == 0) {
            Runtime inst = Runtime.getRuntime();
            long mem_used = inst.totalMemory() - inst.freeMemory();

            if (max_mem - mem_used <= 260 * mb) {
                to_swap = 1;
                System.err.println("Free Memory available is " + (max_mem - mem_used) + " less than 480 MB");
                System.err.println("Memory Used is "+mem_used/mb);
                System.err.println("So Will Start to Swap Out Now");
            }
        }
        if (totalStoredState == 0) {
            Node node = new Node(state);
            tail = head = node;
            hs.add(key);
            totalStoredState++;
        } else {
            Node data = new Node(state);
            if (hs.contains(key)) {
                hash_hits++;
                ret = -1;
            } else {
                if (to_swap == 1) {
                    Node tmp = tail;
                    tail = tail.prev;
                    hs.remove(byteToString(tmp.key));
                    if (wrapper.get(tmp.key) == 0) {
                        wrapper.put(tmp.key);
                        disk_inserts++;
                    }
                    if (wrapper.get(state) == 1) {
                        disk_hits++;
                        ret = -1;
                    }
                    else{
                        totalStoredState++;
                    }
                    disk_searches += 2;
		    tmp = null;
                }
                data.next = head;
                head.prev = data;
                HashMru.head = data;
                hs.add(key);
                totalStoredState++;
            }
        }
        return ret;
    }

    public long getBytes() {
        Runtime inst = Runtime.getRuntime();
        long mem_used = inst.totalMemory() - inst.freeMemory();
        return mem_used;
    }

    public int getStored() {
        return totalStoredState;
        // return stored;
    }

    public void printSummary() {
        System.err.println("----------------------Summary Report--------------------------------\n");
        System.err.println("-------------Mixed MRU-Hashset and Mongodb Implementation --------------------------------\n");
        System.err.println("Number of Hashset Hits: " + hash_hits);
        System.err.println("Number of Disk Hits: " + disk_hits);
        System.err.println("Number of Disk Inserts: " + disk_inserts);
        System.err.println("Number of Disk Searches: " + disk_searches);
        Runtime inst = Runtime.getRuntime();
        long mem_used = inst.totalMemory() - inst.freeMemory();
        System.err.println("Total Memory Used: " + mem_used);
    }

}

class Node {
    byte[] key;
    Node next, prev;

    public Node(byte[] key) {
        this.key = key;
        this.next = null;
        this.prev = null;
    }
}

class JDBCWrapper {
    MongoCollection <Document> coll = null;
    public JDBCWrapper(String Server, int port, String database, String collection) {
        try {
            MongoClient mongoClient = MongoClients.create("mongodb://" + Server + ":" + port);
            MongoDatabase db = mongoClient.getDatabase(database);
            System.out.println("Connect to database successfully");
            CreateCollectionOptions obj = new CreateCollectionOptions();
            MongoCollection tmp = db.getCollection("people");
            tmp.drop(new SingleResultCallback<Void>(){
              @Override
              public void onResult(final Void result, final Throwable t) {
                System.out.println("Deleted");
              }
            });
            db.createCollection("people", new SingleResultCallback<Void>(){
              @Override
              public void onResult(final Void result, final Throwable t) {
                System.out.println("Created");
              }
            });
            coll = db.getCollection("people");
            //BasicDBObject obj1 = new BasicDBObject();
            //obj1.append("key", 1);
            //coll.createIndex(obj1);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void put(byte[] key) {
        Document doc = new Document();
        doc.append("key", key);
        coll.insertOne(doc, new SingleResultCallback<Void>() {
          @Override
          public void onResult(final Void result, final Throwable t) {
            System.out.println("Inserted!");
          }
        });
    }

    public int get(byte[] key) {
        BasicDBObject obj = new BasicDBObject();
        obj.append("key", key);
        MongoCursor curs = coll.find(obj).limit(1).iterator();
        if (queryResult == null) {
            return 1;
        }
        return 0;
    }
}
