package spinja.store;

import java.sql.*;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.BsonDocument;
import org.bson.Document;

public class AVLTree extends StateStore {

    AVLNode root = null;
    static AVLNode head = null;
    AVLNode tail = null;
    JDBCWrapper wrapper;
    int to_swap;
    int totalStoredState;
    long avl_hit_counter;
    long disk_hit_counter;
    long disk_insert_counter;
    long disk_search_counter;

    public AVLTree() {
        wrapper = new JDBCWrapper("localhost", 7777, "test", "spinja");
        totalStoredState = 0;
        avl_hit_counter = 0;
        disk_hit_counter = 0;
        disk_search_counter = 0;
        disk_insert_counter = 0;
        to_swap = 0;
    }

    public int addState(byte[] key) {

        int ret = 1;
        long mb = 1024 * 1024;
        if (to_swap == 0) {
            Runtime inst = Runtime.getRuntime();
            long max_mem = inst.maxMemory();
            long mem_used = inst.totalMemory() - inst.freeMemory();
            if (max_mem - mem_used <= 256 * mb) {
                to_swap = 1;
                System.err.println("Free Memory available is "+(max_mem - mem_used)+" less than 256 MB")
                System.err.println("So Will Start to Swap Out Now");
            }
        }
        if (totalStoredState == 0) {
            root = insert(root, key);
            head = tail = root;
            totalStoredState++;
            return 1;
        } else {
            if (search(root, key)) {
                avl_hit_counter++;
                return -1;
            } else {
                byte[] to_insert = tail.data;
                if (to_swap == 1) {
                    tail = tail.prev;
                    root = delete(root, to_insert);
                    int res1 = wrapper.get(to_insert);
                    int res = wrapper.get(key);
                    disk_search_counter += 2;
                    if (res1 != 1) {
                        disk_insert_counter++;
                        wrapper.put(to_insert);
                    }
                    if (res == 1) {
                        ret = -1;
                        disk_hit_counter++;
                    } else {
                        ret = 1;
                        totalStoredState++;
                    }
                } else {
                    totalStoredState++;
                    ret = 1;
                }
                root = insert(root, key);
                return ret;
            }
        }
    }

    public long getBytes() {
        /* Need to caluclate */
        return maxnodes * 32;
    }

    public int getStored() {
        return totalStoredState;
        // return stored;
    }

    public void printSummary() {
        System.err.println("----------------------Summary Report--------------------------------\n");
        System.err.println("-------------Mixed MRU-AVL and SQLite-Mmap Implementation --------------------------------\n");
        System.err.println("Number of AVL Hits: " + avl_hit_counter);
        System.err.println("Number of Disk Hits: " + disk_hit_counter);
        System.err.println("Number of Disk Inserts: " + disk_insert_counter);
        System.err.println("Number of Disk Searches: " + disk_search_counter);
    }


    private static int height(AVLNode root) {
        return root == null ? 0 : root.height;
    }

    private static int Compare_arrays(byte[] left, byte[] right) {
        int i = 0;
        if (right == null) return -1;
        else if (left == null) return 1;
        while (i < left.length && i < right.length) {
            if (left[i] < right[i]) {
                return -1;
            } else if (left[i] > right[i]) {
                return 1;
            }
            i++;
        }
        if (i != left.length) return -1;
        else if (i != right.length) return 1;
        return 0;
    }

    private static AVLNode insert(AVLNode root, byte[] val) {
        if (root == null) {
            root = new AVLNode(val);
            root.prev = AVLTree.head;
            AVLTree.head = root;
        } else if (Compare_arrays(val, root.data) < 0) {
            root.left = insert(root.left, val);
            int ltht = height(root.left);
            int rtht = height(root.right);
            if (ltht - rtht == 2)
                if (Compare_arrays(val, root.data) < 0)
                    root = rotateWithLeftChild(root);
                else
                    root = doubleWithLeftChild(root);
        } else if (Compare_arrays(val, root.data) > 0) {
            root.right = insert(root.right, val);
            int ltht = height(root.left);
            int rtht = height(root.right);
            if (rtht - ltht == 2)
                if (Compare_arrays(val, root.data) > 0)
                    root = rotateWithRightChild(root);
                else
                    root = doubleWithRightChild(root);
        } else
            ;
        int ltht = height(root.left);
        int rtht = height(root.right);
        root.height = Math.max(ltht, rtht) + 1;
        return root;
    }


    private static AVLNode rotateWithLeftChild(AVLNode root) {
        AVLNode temp = root.left;
        root.left = temp.right;
        temp.right = root;
        root.height = Math.max(height(root.left), height(root.right)) + 1;
        temp.height = Math.max(height(temp.left), height(temp.right)) + 1;
        return temp;
    }

    private static AVLNode rotateWithRightChild(AVLNode root) {
        AVLNode temp = root.right;
        root.right = temp.left;
        temp.left = root;
        root.height = Math.max(height(root.left), height(root.right)) + 1;
        temp.height = Math.max(height(temp.left), height(temp.right)) + 1;
        return temp;
    }

    private static AVLNode doubleWithLeftChild(AVLNode root) {
        root.left = rotateWithRightChild(root.left);
        return rotateWithLeftChild(root);
    }

    private static AVLNode doubleWithRightChild(AVLNode root) {
        root.right = rotateWithLeftChild(root.right);
        return rotateWithRightChild(root);
    }

    private static void postOrderTraversal(AVLNode root) {
        if (root != null) {
            postOrderTraversal(root.left);
            System.out.println("Value is " + root.data + " and height is " + root.height);
            postOrderTraversal(root.right);
        }
    }

    private static void preOrderTraversal(AVLNode root) {
        if (root != null) {
            System.out.println("Value is " + root.data + " and height is " + root.height);
            preOrderTraversal(root.left);
            preOrderTraversal(root.right);
        }
    }

    public static AVLNode delete(AVLNode root, byte[] val) {
        AVLNode temp = root;
        if (root == null)
            return null;
        if (root.data.equals(val)) {
            if (temp.left != null) {
                temp = temp.left;
                while (temp.right != null) {
                    temp = temp.right;
                }
                byte[] tempdata = temp.data;
                root.left = delete(root.left, temp.data);
                root.data = tempdata;
                if (root.left == null && root.right == null)
                    root.height = 1;
                int ltht = height(root.left);
                int rtht = height(root.right);
                if (ltht - rtht == -2) {
                    //System.out.println("Got Imbalance at " + root.data + " and val is  " + val);
                    if (height(root.right.right) >= height(root.right.left))
                        root = rotateWithRightChild(root);
                    else
                        root = doubleWithRightChild(root);
                }
            } else if (temp.right != null) {
                temp = temp.right;
                while (temp.left != null) {
                    temp = temp.left;
                }
                byte[] tempdata = temp.data;
                root.right = delete(root.right, temp.data);
                root.data = tempdata;
                if (root.left == null && root.right == null)
                    root.height = 1;
                int ltht = height(root.left);
                int rtht = height(root.right);
                if (rtht - ltht == -2) {
                    //System.out.println("Got Imbalance at " + root.data + " and val is " + val);
                    if (height(root.left.right) <= height(root.left.left)) {
                        root = rotateWithLeftChild(root);
                    } else
                        root = doubleWithLeftChild(root);
                }
            } else {
                return null;
            }
        } else if (Compare_arrays(val, root.data) > 0) {
            root.right = delete(root.right, val);
        } else {
            root.left = delete(root.left, val);
        }
        int ltht = height(root.left);
        int rtht = height(root.right);
        root.height = ltht > rtht ? ltht + 1 : rtht + 1;
        return root;
    }

    private static boolean search(AVLNode root, byte[] val) {
        boolean found = false;
        AVLNode tmp = root;
        while ((tmp != null) & !found) {
            byte[] temp = tmp.data;
            if (Compare_arrays(temp, val) > 0) {
                tmp = tmp.left;
            } else if (Compare_arrays(temp, val) < 0) {
                tmp = tmp.right;
            } else {
                found = true;
            }
        }
        return found;
    }
}

class AVLNode {
    AVLNode left, right, next, prev;
    int height;
    byte[] data;

    public AVLNode() {
        this.left = this.right = this.next = this.prev = null;
        this.data = null;
        this.height = 1;
    }

    public AVLNode(byte[] n) {
        this.left = this.right = this.next = this.prev = null;
        this.data = n;
        this.height = 1;
    }
}

/**
 * Created by harry7 on 13/7/16.
 */
class JDBCWrapper {
    MongoCollection coll = null;

    public JDBCWrapper(String Server, int port, String database, String collection) {
        try {

            MongoClient mongoClient = new MongoClient(Server, port);
            MongoDatabase db = mongoClient.getDatabase(database);
            System.out.println("Connect to database successfully");
            CreateCollectionOptions obj = new CreateCollectionOptions();
            MongoCollection tmp = db.getCollection("people");
            tmp.drop();
            db.createCollection("people");
            coll = db.getCollection("people");
            BasicDBObject obj1 = new BasicDBObject();
            obj1.append("key", 1);
            coll.createIndex(obj1);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void put(byte[] key) {
        Document doc = new Document();
        doc.append("key", key);
        coll.insertOne(doc);
    }

    public int get(byte[] key) {
        BasicDBObject obj = new BasicDBObject();
        obj.append("key", key);
        MongoCursor curs = coll.find(obj).limit(1).iterator();
        if (curs.hasNext()) {
            return 1;
        }
        return 0;
    }
}