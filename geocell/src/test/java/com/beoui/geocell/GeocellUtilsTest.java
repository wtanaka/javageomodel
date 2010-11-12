package com.beoui.geocell;

import static org.junit.Assert.*;

import org.junit.Test;

import com.beoui.geocell.model.Point;

/**
 * Port of http://code.google.com/p/geomodel/source/browse/trunk/geo/geocell_test.py
 * @author edgar.dalmacio@gmail.com
 */
public class GeocellUtilsTest {

	private Point point = new Point(37, -122);
	
	@Test
	public void testCompute() {
		// a valid geocell
		String cell = GeocellUtils.compute(point, 14);
		assertEquals(14, cell.length());
		assertTrue(GeocellUtils.isValid(cell));
		assertTrue(GeocellUtils.containsPoint(cell, point));
		
		// a lower resolution cell should be a prefix to a higher resolution
		// cell containing the same point
		String lowresCell = GeocellUtils.compute(point, 8);
	    assertTrue(cell.startsWith(lowresCell));
	    assertTrue(GeocellUtils.containsPoint(lowresCell, point));
	    
	    // an invalid geocell
	    cell = GeocellUtils.compute(new Point(0, 0), 0);
	    assertEquals(0, cell.length());
	    assertFalse(GeocellUtils.isValid(cell));
	}
	
}
