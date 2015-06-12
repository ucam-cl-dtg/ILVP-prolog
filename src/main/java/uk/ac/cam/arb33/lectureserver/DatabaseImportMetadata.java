/*
 * A command-line utility to import textual data directly into a cell in PostgreSQL.
 *
 * Copyright 2012 Alastair R. Beresford
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero Public License for more details.
 */
package uk.ac.cam.arb33.lectureserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.io.FileUtils;

/**
 * Import metadata from a file and insert it into the correct place in the database
 * 
 * @author Alastair Beresford
 *
 */
public class DatabaseImportMetadata {

	final private static String DB_URL = "jdbc:postgresql://localhost/lectures";
	final private static String DB_USERNAME = "lectures";
	final private static String DB_PASSWORD = "";

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Args required: <Filename of data>  <id of row in database>");
			return;
		}

	  String data = FileUtils.readFileToString(new File(args[0]));	  
		int id = Integer.parseInt(args[1]);
		Class.forName("org.postgresql.Driver");
		Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		String sql = "UPDATE activity SET metadata=? WHERE id=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setString(1, data);
		ps.setInt(2, id);
		ps.executeUpdate();
		ps.close();
		
		sql = "SELECT metadata FROM activity WHERE id=?";
		ps = conn.prepareStatement(sql);
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		if(rs.next()) {
			System.out.println(rs.getString(1));
		}
		
		ps.close();
		conn.close();
	}
}