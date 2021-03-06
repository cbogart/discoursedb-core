/*******************************************************************************
 * Copyright (C)  2015 - 2016  Carnegie Mellon University
 * Author: Oliver Ferschke
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.annotation.lightside.io;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.cmu.cs.lti.discoursedb.configuration.BaseConfiguration;
import edu.cmu.cs.lti.discoursedb.core.type.DiscoursePartTypes;

/**
 * Exports annotated training data to LightSide. 
 * 
 * @author Oliver Ferschke
 */
@Component
@SpringBootApplication
@ComponentScan(basePackages = { "edu.cmu.cs.lti.discoursedb.configuration", "edu.cmu.cs.lti.discoursedb.annotation.lightside.io" }, useDefaultFilters = false, includeFilters = {
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { LightSideTrainingDataExport.class, BaseConfiguration.class, LightSideService.class }) })
public class LightSideTrainingDataExport implements CommandLineRunner{

	@Autowired private LightSideService lsService;	
	
	/**
	 * Launches the SpringBoot application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Assert.isTrue(args.length >=2 && args.length<=3, "USAGE: LightSideTrainingDataExport <DiscourseName> <outputFile> <DiscoursePart type to extract (default: THREAD)>");
		SpringApplication.run(LightSideTrainingDataExport.class, args);
	}

	@Override
	@Transactional 
	public void run(String... args) throws Exception {
		String discourseName = args[0];
		Assert.hasText(discourseName, "Discourse name cannot be empty.");

		String outputFilePath = args[1];
		Assert.hasText(outputFilePath, "Path to the output file cannot be empty.");		
		
		File outputFile = new File(outputFilePath);
		Assert.isTrue(!outputFile.isDirectory(), outputFilePath+" points to a directory but has to point to a file.");
		
		DiscoursePartTypes dptype = DiscoursePartTypes.THREAD;
		if(args.length==3){
			dptype = DiscoursePartTypes.valueOf(args[2]);
		}
		
		lsService.exportAnnotations(discourseName, dptype, outputFile);
	}
}
