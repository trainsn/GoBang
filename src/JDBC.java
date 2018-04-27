//STEP 1. Import required packages
import java.sql.*;

import sun.reflect.generics.tree.VoidDescriptor;

public class JDBC {
   // JDBC driver name and database URL
   static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
   static final String DB_URL = "jdbc:mysql://localhost/game";

   //  Database credentials
   static final String USER = "root";
   static final String PASS = "123456a";
   
   public  void insertRecord(char win) {
   Connection conn = null;
   Statement stmt = null;
   try{
      //STEP 2: Register JDBC driver
      Class.forName("com.mysql.jdbc.Driver");

      //STEP 3: Open a connection
      //System.out.println("Connecting to a selected database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      //System.out.println("Connected database successfully...");
      
      //STEP 4: Execute a query
      //System.out.println("Inserting records into the table...");
      stmt = conn.createStatement();
      
      String sql = "INSERT INTO record " +
                   "VALUES ("+ System.currentTimeMillis() / 1000 + ",'"+ win+ "');";
      //System.out.println(sql);
      stmt.executeUpdate(sql);
      
      //System.out.println("Inserted records into the table...");

   }catch(SQLException se){
      //Handle errors for JDBC
      se.printStackTrace();
   }catch(Exception e){
      //Handle errors for Class.forName
      e.printStackTrace();
   }finally{
      //finally block used to close resources
      try{
         if(stmt!=null)
            conn.close();
      }catch(SQLException se){
      }// do nothing
      try{
         if(conn!=null)
            conn.close();
      }catch(SQLException se){
         se.printStackTrace();
      }//end finally try
   }//end try
   //System.out.println("Goodbye!");
}//end main
}//end JDBCExample