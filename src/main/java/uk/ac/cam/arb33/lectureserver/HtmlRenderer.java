/*
 * Abstracts the rendering of HTML pages and the underlying template technology.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import com.google.common.io.InputSupplier;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;

class HtmlRenderer {

	final private SoyTofu tofu;

	HtmlRenderer(final InputStream templateStream) {
		
		SoyFileSet sfs = new SoyFileSet.Builder().add(new InputSupplier<InputStreamReader>() {
			public InputStreamReader getInput() throws IOException {
				return new InputStreamReader(templateStream);
			}
		}, "Interal war resource: " + templateStream).build();
		tofu = sfs.compileToTofu().forNamespace("uk.ac.cam.arb33.lectureserver.soy");		
	}

	/**
	 * Construct the set of global variables for all templates
	 * @param req
	 * @param r
	 * @return
	 */
	private Renderer ijData(PageMetadata metadata, final Renderer r) {

		SoyListData breadcrumb = new SoyListData();
		for(int i = 2; i < metadata.breadcrumb.length; i++) {
			breadcrumb.add(metadata.breadcrumb[i]);
		}
		SoyListData breadcrumbUrl = new SoyListData();
		for(int i = 2; i < metadata.breadcrumbUrl.length; i++) {
			breadcrumbUrl.add(metadata.breadcrumbUrl[i]);
		}
		//TODO(arb33): ...Write a class map tool which uses reflection to do the following
		SoyMapData map = new SoyMapData(
				"idPerson", metadata.idPerson,
				"personFullName", metadata.personFullName,
				"personCrsid", metadata.personCrsid,
				"uri", metadata.uri,
				"contextPath", metadata.contextPath,
				"servletPath", metadata.servletPath,
				"contextAndServletPath", metadata.contextAndServletPath,
				"breadcrumb", breadcrumb,
				"breadcrumbUrl", breadcrumbUrl);
		//TODO(arb33): rewrite strings in camel case (e.g. UNDEFINED_EVENT -> undefinedEvent)
		//TODO(arb33): this is probably better abstracted in PageMetadata so there isn't a db ref here.
		SoyListData events = new SoyListData();
		for(Database.EventType e: Database.EventType.values()) {
			events.add(new SoyMapData("key", "" + e, "value", e.getId()));
		}
		map.put("events", events);
		r.setIjData(map);
		return r;
	}
	
	void homePage(PageMetadata metadata, PrintWriter out) throws IOException {

		out.write(ijData(metadata, tofu.newRenderer(".homepage")).render());
	}


	void allCourseInYear(PageMetadata metadata, PrintWriter out, List<Database.Course> courses) 
		throws FileNotFoundException {

		if (courses == null || courses.size() == 0) {
			throw new FileNotFoundException();
		}

		SoyListData list = new SoyListData();
		for(Database.Course c: courses) {
			list.add(new SoyMapData("urlTitle", c.urlTitle, "fullTitle", c.fullTitle));
		}
		SoyMapData arguments = new SoyMapData("list", list);
		out.write(ijData(metadata, tofu.newRenderer(".courseThisYear")).setData(arguments).render());		
	}

	void course(PageMetadata metadata, PrintWriter out, List<Database.Activity> activities,
			String fullTitle) throws FileNotFoundException {
		
		if (activities == null || activities.size() == 0) {
			throw new FileNotFoundException();
		}

		SoyListData list = new SoyListData();
		String currentSection = null;
		SoyListData section = null;
		for(Database.Activity a: activities) {
			if(currentSection == null || !currentSection.equals(a.section)) {
				if(section != null) {
					list.add(section);
				}
				section = new SoyListData();
				currentSection = a.section;
			}
			section.add(new SoyMapData("id", a.id, "urlTitle", a.urlTitle, "fullTitle", a.fullTitle, 
					"description", a.description, "urlType", a.urlType, "complete", a.complete,
					"section", a.section, "activityTime", a.activityTime));
		}
		if(section != null) {
			list.add(section);
		}		
		SoyMapData arguments = new SoyMapData("list", list, "title", fullTitle);
		out.write(ijData(metadata, tofu.newRenderer(".course")).setData(arguments).render());		
	}

	void studentProgress(PageMetadata metadata, PrintWriter out, Database.StudentProgress progress) 
			throws FileNotFoundException {
		
		if (progress == null) {
			throw new FileNotFoundException();
		}
		
		SoyListData activities = new SoyListData();
		for(Database.Activity a: progress.activities) {
			activities.add(new SoyMapData("id", a.id, "urlTitle", a.urlTitle, "fullTitle", a.fullTitle, 
					"description", a.description, "urlType", a.urlType, "complete", a.complete));
		}
		
		SoyListData people = new SoyListData();
		for(Database.Person p: progress.people) {
			String crsid = p.email.split("@")[0];
			people.add(new SoyMapData("id", p.id, "firstname", p.firstname, "familyname", p.familyname,
					"crsid", crsid));
		}
		
		SoyListData completed = new SoyListData();
		for(boolean[] c: progress.completed) {
			SoyListData row = new SoyListData();
			for(boolean b: c) {
				row.add(b);
			}
			completed.add(row);
		}

		SoyMapData course = new SoyMapData("urlTitle", progress.course.urlTitle, "urlVersion",
				progress.course.urlVersion);
		
		SoyMapData arguments = new SoyMapData("activities", activities, "people", people, 
				"completed", completed, "course", course);
		out.write(ijData(metadata, tofu.newRenderer(".supervisorPage")).setData(arguments).render());
	}
	
	void courseActivity(PageMetadata metadata, PrintWriter out, Database.Activity a) 
			throws FileNotFoundException {

		if (a == null) {
			throw new FileNotFoundException();
		}
		//TODO(arb33): metadata no longer being read from DB but from flat file; neaten this up.
		String videoName = metadata.breadcrumb[3];
		
		//TODO(arb33): need to cope with more than one type of activity
		//TODO(arb33): abstract out the video url and put it somewhere (DB?)
		SoyMapData arguments = new SoyMapData("title", a.fullTitle,  "urlVideo", 
				metadata.contextPath + "/video/" + a.urlTitle, 
				"description", a.description, "metadata", videoName);
		out.write(ijData(metadata, tofu.newRenderer(".videoActivity")).setData(arguments).render());
	}
}