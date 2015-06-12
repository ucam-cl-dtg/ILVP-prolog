/*
 * Database.java abstracts the underlying database technology and data layout.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Database {

	//TODO(arb33): Put a check into constructor to ensure student table has id=1 entry...
	public static final int SYSTEM_ID = 1;
	private static Database db;
	private DataSource dataSource;

	/**
	 * Mirrors EventType stored in the Database; should be kept in sync with it
	 */
	public enum EventType {

		//TODO(arb33): Check when DB is loaded that these elements are in EventType table
		SYSTEM(1),
		UNDEFINED_EVENT(2),
		ACTIVITY_COMPLETE(3),
		PAGE_SUCCESS(4),
		VIDEO_PLAY(5),
		VIDEO_PAUSE(6),
		VIDEO_TIMER_TICK(7),
		VIDEO_LOAD(8),
		VIDEO_CURRENT_TIME(9),
		SLIDE_CHANGE_RELATIVE(10),
		SLIDE_CHANGE_ABSOLUTE(11),
		SLIDE_SEEK_RELATIVE(12),
		QUESTION_CHANGE_RELATIVE(13),
		QUESTION_CHANGE_ABSOLUTE(14),
		QUIZ_INCORRECT(15),
		QUIZ_CONTINUE(16),
		QUIZ_CORRECT(17);
		private final int id;
		EventType(int id) {
			this.id = id;
		}
		int getId() {
			return id;
		}
	}

	public enum PersonType {
		SYSTEM(1),
		ADMIN(2),
		SUPERVISOR(3),
		STUDENT(4),
		GUEST(5);
		private final int id;
		PersonType(int id) {
			this.id = id;
		}
		int getId() {
			return id;
		}
	}

	public enum AuthType {
		OPEN(1),
		AUTO_CREATE(2),
		EXISTING_ACCOUNT(3),
		ERROR(4);
		private final int id;
		AuthType(int id) {
			this.id = id;
		}
		int getId() {
			return id;
		}
	}

	public class Activity {
		public int id;
		public String urlType; //Single word; activity type, e.g. "video"; used in URL
		public String urlTitle; //Single word; unique for given activity type; used in URL
		public String fullTitle;
		public String description;
		public String section;
		public String activityTime;
		public boolean complete;
		public String metadata; //JSON info used to render activity
	}

	public class Course {
		public int id;
		public String urlTitle;
		public String fullTitle;
		public String urlVersion;
		public String fullVersion;
	}

	public class Person {
		public int id;
		public String firstname;
		public String familyname;
		public String email;
		public String session;
	}

	public class StudentProgress {
		public List<Person> people;
		public List<Activity> activities;
		public Course course;
		public List<boolean[]> completed;
	}

	private Database() throws SQLException, NamingException {
		Context ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/prolog1415");
	}

	synchronized public static Database getDatabase() throws SQLException, NamingException {
		if (db == null) {
			db = new Database();
		}
		return db;
	}

	private Activity resultSetToActivity(ResultSet rs) throws SQLException {
		Activity a = new Activity();
		a.id = rs.getInt(1);
		a.urlType = rs.getString(2);
		a.urlTitle = rs.getString(3);
		a.fullTitle = rs.getString(4);
		a.description = rs.getString(5);
		a.complete = rs.getBoolean(6);
		a.metadata = rs.getString(7);
		a.section = rs.getString(8);
		a.activityTime = rs.getString(9);
		return a;
	}

	/**
	 * 
	 */
	public AuthType getAuthType() throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		AuthType authType = AuthType.ERROR;
		try {
			String sql = "SELECT configValue FROM config WHERE configKey = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, "authType");
			ResultSet rs = ps.executeQuery();

			//Assume there is only one match; this is enforced by the database
			if(rs.next()) {
				String authValue = rs.getString(1);
				if (authValue.equals("Open"))
					authType = AuthType.OPEN;
				if (authValue.equals("AutoCreate"))
					authType = AuthType.AUTO_CREATE;
				if (authValue.equals("ExistingAccount"))
					authType = AuthType.EXISTING_ACCOUNT;
			}
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
		return authType;
	}

	private String getDbConfig(String key) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		String value = null;
		try {
			String sql = "SELECT configValue FROM config WHERE configKey = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, key);
			ResultSet rs = ps.executeQuery();

			//Assume there is only one match; this is enforced by the database
			if(rs.next()) {
				value = rs.getString(1);
			}
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
		return value;
	}

	public String getDefaultHomePage() throws SQLException {
		return getDbConfig("defaultHomePage");
	}

	public String getSoyTemplate() throws SQLException {
		return getDbConfig("soyTemplate");
	}

	
	/**
	 * 
	 */
	public String getVideoUrlBase() throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		String url = "";
		try {
			String sql = "SELECT configValue FROM config WHERE configKey = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, "videoUrlBase");
			ResultSet rs = ps.executeQuery();

			//Assume there is only one match; this is enforced by the database
			if(rs.next()) {
				url = rs.getString(1);
			}
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
		return url;
	}
	/**
	 * Retrieve list of activities for a course, including whether they have been completed
	 * 
	 */
	public List<Activity> getCourseActivities(int idPerson, String urlCourse, String urlVersion) 
			throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT activity.id, activity.urlType, activity.urlTitle, activity.fullTitle, " +
					"description, complete.idPerson IS NOT NULL AS done, metadata, section, activityTime FROM activity " +
					"LEFT JOIN (SELECT idActivity, idPerson FROM activityComplete WHERE idPerson=?) " +
					"AS complete ON complete.idActivity = activity.id "+ 
					"JOIN course ON activity.idCourse = course.id " +
					"WHERE course.urlTitle=? AND course.urlVersion=? ORDER BY activity.index ASC";
			ps = conn.prepareStatement(sql);
			ps.setInt(1, idPerson);
			ps.setString(2, urlCourse);
			ps.setString(3, urlVersion);
			ResultSet rs = ps.executeQuery();
			ArrayList<Activity> as = new ArrayList<Activity>();			
			while(rs.next()) { //Assumes there is at most one result; DB constraints enforce this
				as.add(resultSetToActivity(rs));
			}
			return as;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}

	public List<Activity> getCourseActivities(String urlCourse, String urlVersion) 
			throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT activity.id, activity.urlType, activity.urlTitle, activity.fullTitle, " +
					"description, NULL AS done, metadata, section, activityTime FROM activity " +
					"JOIN course ON activity.idCourse = course.id " +
					"WHERE course.urlTitle=? AND course.urlVersion=? ORDER BY activity.index ASC";
			ps = conn.prepareStatement(sql);
			ps.setString(1, urlCourse);
			ps.setString(2, urlVersion);
			ResultSet rs = ps.executeQuery();
			ArrayList<Activity> as = new ArrayList<Activity>();			
			while(rs.next()) {
				as.add(resultSetToActivity(rs));
			}
			return as;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}		
	}

	public List<Person> getStudents(int idSupervisor, String urlCourse, String urlVersion) 
			throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT person.id, firstname, familyname, email FROM " +
					"supervisor JOIN course ON idCourse=id JOIN person ON idstudent=person.id WHERE " +
					"urlTitle=? AND urlVersion=? AND idsupervisor=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, urlCourse);
			ps.setString(2, urlVersion);
			ps.setInt(3, idSupervisor);
			ResultSet rs = ps.executeQuery();
			ArrayList<Person> students = new ArrayList<Person>();
			while(rs.next()) { //Assumes there is at most one result; DB constraints enforce this
				Person s = new Person();
				s.id = rs.getInt(1);
				s.firstname = rs.getString(2);
				s.familyname = rs.getString(3);
				s.email = rs.getString(4);
				students.add(s);
			}
			return students;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}		

	}

	/**
	 * Retrieve every student of a supervisor for a course, including progress towards activities.
	 * 
	 * Data is returned sorted by student surname.
	 */
	public StudentProgress getStudentProgress(int idSupervisor, String urlCourse, String urlVersion)
			throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			StudentProgress sp = new StudentProgress();
			sp.people = getStudents(idSupervisor, urlCourse, urlVersion);
			sp.course = getCourse(urlCourse, urlVersion);
			sp.completed = new ArrayList<boolean[]>();
			for(Person p: sp.people) {
				List<Activity> activities = getCourseActivities(p.id, urlCourse, urlVersion);
				if (sp.activities == null) {
					//Assumes all students are given same activities for given course
					sp.activities = activities; 
				}

				boolean[] completed = new boolean[activities.size()];
				int index = 0;
				for(Activity a: activities) {
					completed[index++] = a.complete;
				}
				sp.completed.add(completed);
			}
			if (sp.activities == null) {
				//For consistency with other variables on this object, no activities is a zero-length list
				sp.activities = new ArrayList<Activity>();
			}
			return sp;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}		
	}


	/**
	 * Retrieve details of a specific activity from the database
	 * 
	 * @param urlActivityType
	 * @param urlActivityTitle
	 * @return
	 * */
	public Activity getActivity(int idPerson, String urlCourse, String urlVersion, 
			String urlActivityType, String urlActivityTitle) throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT activity.id, activity.urlType, activity.urlTitle, activity.fullTitle, " +
					"description, complete.idPerson IS NOT NULL AS done, metadata, section, activityTime FROM activity " +
					"LEFT JOIN (SELECT idActivity, idPerson FROM activityComplete WHERE idPerson=?) " +
					"AS complete ON complete.idActivity = activity.id "+ 
					"JOIN course ON activity.idCourse = course.id " +
					"WHERE course.urlTitle=? AND course.urlVersion=? " +
					"AND activity.urlType=? AND activity.urlTitle=?";
			ps = conn.prepareStatement(sql);
			ps.setInt(1, idPerson);
			ps.setString(2, urlCourse);
			ps.setString(3, urlVersion);
			ps.setString(4, urlActivityType);
			ps.setString(5, urlActivityTitle);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) { //Assumes there is at most one result; DB constraints enforce this
				return resultSetToActivity(rs);
			} 
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
		return null;
	}

	/**
	 * Retrieve the full title for a course given it's url form
	 */
	public String getCourseTitle(String urlCourse, String urlVersion) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT fullTitle FROM course WHERE urlTitle=? AND urlVersion=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, urlCourse);
			ps.setString(2, urlVersion);
			ResultSet rs = ps.executeQuery();
			String result = null;
			if(rs.next()) {
				result = rs.getString(1);
			}
			return result;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}

	}

	/**
	 * Retrieve list of course names for a particular presentation
	 */
	public Course getCourse(String urlTitle, String urlVersion) throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT id, fullTitle FROM course WHERE urlVersion=? AND urlTitle=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, urlVersion);
			ps.setString(2, urlTitle);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				Course c = new Course();
				c.id = rs.getInt(1);
				c.urlTitle = urlTitle;
				c.fullTitle = rs.getString(2);
				c.urlVersion = urlVersion;
				return c;
			}
			return null;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}	

	/**
	 * Retrieve list of course names for a particular presentation
	 */
	public List<Course> getCourses(String urlVersion) throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT id, urlTitle, fullTitle FROM course WHERE urlVersion=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, urlVersion);
			ResultSet rs = ps.executeQuery();
			ArrayList<Course> course = new ArrayList<Course>();
			while(rs.next()) {
				Course c = new Course();
				c.id = rs.getInt(1);
				c.urlTitle = rs.getString(2);
				c.fullTitle = rs.getString(3);
				c.urlVersion = urlVersion;
				course.add(c);
			}
			return course;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}

	public Database.Person getPerson(String email) throws SQLException{
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "SELECT id, firstname, familyname, email, session FROM person WHERE email=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1,	email);
			ResultSet rs = ps.executeQuery();
			Person s = null;
			if (rs.next()) {
				s = new Person();
				s.id = rs.getInt(1);
				s.firstname = rs.getString(2);
				s.familyname = rs.getString(3);
				s.email = rs.getString(4);
				s.session = rs.getString(5);
			}
			return s;
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}

	public Person findOrAddPerson(String firstname, String familyname, String email, String session) throws SQLException {
			
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			Integer emailInDb = null;
			Integer sessionInDb = null;

			String sqlCheckForSession = "SELECT id, firstname, familyname, email, session FROM person WHERE session=? OR email=?";
			ps = conn.prepareStatement(sqlCheckForSession);
			ps.setString(1, session);
			ps.setString(2, email);
			{
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					if (rs.getString(4) != null) emailInDb = rs.getInt(1);
					if (rs.getString(5) != null) sessionInDb = rs.getInt(1);
					if (rs.getString(2) != null && firstname == null) firstname = rs.getString(2);
					if (rs.getString(3) != null && familyname == null) familyname = rs.getString(3);
				}
			}
			ps.close();
			ps = null;

			int personId;
			if (emailInDb != null) {
				personId = emailInDb;
				//TODO(arb33): Update DB with most recent copy of firstname and family name?
				//TODO(arb33): If old personSessionId exists, should we merge?
			} else {
				if (sessionInDb != null) {
					personId = sessionInDb;
					if (email != null) {
						String sql = "UPDATE person SET firstname=?, familyname=?, email=?, session=? WHERE session=?";
						ps = conn.prepareStatement(sql);
						ps.setString(1, firstname);
						ps.setString(2, familyname);
						ps.setString(3, email);
						ps.setString(4, null);
						ps.setString(5, session);
						//ps.setString(4, null);
						ps.executeUpdate();
					}
				} else {
					AuthType authType = getAuthType();
					if (authType == AuthType.EXISTING_ACCOUNT)
						return null;
					if (authType == AuthType.AUTO_CREATE && email == null)
						return null;

					String sql = "INSERT INTO person (firstname, familyname, email, session, idpersonrole) VALUES (?, ?, ?, ?, ?) RETURNING id";
					ps = conn.prepareStatement(sql);
					ps.setString(1, firstname);
					ps.setString(2, familyname);
					ps.setString(3,  email);
					ps.setString(4,  session);
					ps.setInt(5,  PersonType.STUDENT.getId());
					ResultSet rs = ps.executeQuery();
					if (rs.next()) {
						personId = rs.getInt(1);
					} else {
						throw new SQLException("LectureServer: Inconsistent database - out of file space?");
					}
				}
			}

			Person p = new Person();
			p.id = personId;
			p.firstname = firstname;
			p.familyname = familyname;
			p.email = email;
			p.session = session;
			return p;

		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}


	public void putEvent(int idPerson, int idEventType, String value) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO event (idPerson, idEventType, value) VALUES (?, ?, ?)";
			ps = conn.prepareStatement(sql);
			ps.setInt(1, idPerson);
			ps.setInt(2, idEventType);
			ps.setString(3, value);
			ps.executeUpdate();
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}

	/**
	 * Set or unset a flag indicating whether a specific student has completed an activity
	 * 
	 * @param urlType
	 * @param urlTitle
	 * @param activityComplete
	 */
	void putActivityComplete(int idPerson, int idActivity, boolean activityComplete) 
			throws SQLException {

		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		try {
			String sql;
			if (activityComplete) {
				sql = "INSERT INTO activityComplete (idPerson, idActivity) VALUES (?, ?)";					
			} else {
				sql = "DELETE FROM activityComplete WHERE idPerson=? AND idActivity=?";
			}
			ps = conn.prepareStatement(sql);
			ps.setInt(1, idPerson);
			ps.setInt(2, idActivity);
			ps.executeUpdate();
		} finally {
			conn.close();
			if (ps != null) {
				ps.close();
			}
		}
	}
}