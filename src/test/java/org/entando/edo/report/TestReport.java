/*
*
* Copyright 2013 Entando S.r.l. (http://www.entando.com) All rights reserved.
*
* This file is part of Entando Enterprise Edition software.
* You can redistribute it and/or modify it
* under the terms of the Entando's EULA
* 
* See the file License for the specific language governing permissions   
* and limitations under the License
* 
* 
* 
* Copyright 2013 Entando S.r.l. (http://www.entando.com) All rights reserved.
*
*/
package org.entando.edo.report;

import org.entando.edo.report.Report;

import junit.framework.TestCase;


public class TestReport extends TestCase {

	
	public void testReport() throws Exception {
		Report report = Report.getInstance();
		report.addFile("10");
		report.addFile("20");
		report.addFileToMerge("a");
		Report report2 = Report.getInstance();
		assertEquals(report, report2);
		report2.addFile("30");
		report2.addFileToMerge("b");
		report2.addFileToMerge("c");
		Report report3 = Report.getInstance();
		assertEquals(report3, report2);
		assertEquals(3, report3.getFiles().size());
		assertEquals(3, report3.getFilesToMerge().size());
	}
	
}
