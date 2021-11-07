package com.christopher.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PageRetriever {
    static final String DB_URL = "jdbc:mysql://localhost/Web_Pages";
    static final String USER = "root";
    static final String PASS = "password";
    
    public String getPage(int docId){
        String text = "";
        try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);) {		      
            String query = "Select url from page_table where doc_id =? limit 1";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setInt(1, docId);
            ResultSet rs = preparedStmt.executeQuery();
            
            while (rs.next()) {
                text = rs.getString("url");   
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } 
        return text;
    }
}
