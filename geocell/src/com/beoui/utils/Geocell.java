/*
Copyright 2010 Alexandre Gellibert 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.apache.org/licenses/
LICENSE-2.0 Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an "AS IS" 
BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing permissions 
and limitations under the License.
*/
package com.beoui.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
#
# Copyright 2010 Alexandre Gellibert
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/


/**
 * Ported java version of python geocell: http://code.google.com/p/geomodel/source/browse/trunk/geo/geocell.py
 * 
 * Defines the notion of 'geocells' and exposes methods to operate on them.

	A geocell is a hexadecimal string that defines a two dimensional rectangular
	region inside the [-90,90] x [-180,180] latitude/longitude space. A geocell's
	'resolution' is its length. For most practical purposes, at high resolutions,
	geocells can be treated as single points.

	Much like geohashes (see http://en.wikipedia.org/wiki/Geohash), geocells are
	hierarchical, in that any prefix of a geocell is considered its ancestor, with
	geocell[:-1] being geocell's immediate parent cell.

	To calculate the rectangle of a given geocell string, first divide the
	[-90,90] x [-180,180] latitude/longitude space evenly into a 4x4 grid like so:

	             +---+---+---+---+ (90, 180)
	             | a | b | e | f |
	             +---+---+---+---+
	             | 8 | 9 | c | d |
	             +---+---+---+---+
	             | 2 | 3 | 6 | 7 |
	             +---+---+---+---+
	             | 0 | 1 | 4 | 5 |
	  (-90,-180) +---+---+---+---+

	NOTE: The point (0, 0) is at the intersection of grid cells 3, 6, 9 and c. And,
	      for example, cell 7 should be the sub-rectangle from
	      (-45, 90) to (0, 180).

	Calculate the sub-rectangle for the first character of the geocell string and
	re-divide this sub-rectangle into another 4x4 grid. For example, if the geocell
	string is '78a', we will re-divide the sub-rectangle like so:

	               .                   .
	               .                   .
	           . . +----+----+----+----+ (0, 180)
	               | 7a | 7b | 7e | 7f |
	               +----+----+----+----+
	               | 78 | 79 | 7c | 7d |
	               +----+----+----+----+
	               | 72 | 73 | 76 | 77 |
	               +----+----+----+----+
	               | 70 | 71 | 74 | 75 |
	  . . (-45,90) +----+----+----+----+
	               .                   .
	               .                   .

	Continue to re-divide into sub-rectangles and 4x4 grids until the entire
	geocell string has been exhausted. The final sub-rectangle is the rectangular
	region for the geocell.
 * 
 * @author api.roman.public@gmail.com (Roman Nurik)
 * @author (java portage) Alexandre Gellibert
 * 
 * 
 */

public class Geocell {

	// Geocell algorithm constants.
	public static final int GEOCELL_GRID_SIZE = 4;
	private static final String GEOCELL_ALPHABET = "0123456789abcdef";

	// The maximum *practical* geocell resolution.
	public static final int MAX_GEOCELL_RESOLUTION = 13;

	// The maximum number of geocells to consider for a bounding box search.
	private static final int MAX_FEASIBLE_BBOX_SEARCH_CELLS = 300;

	// Direction enumerations.
	private static final int[] NORTHWEST = new int[] {-1,1};
	private static final int[] NORTH = new int[] {0,1};
	private static final int[] NORTHEAST = new int[] {1,1};
	private static final int[] EAST = new int[] {1,0};
	private static final int[] SOUTHEAST = new int[] {1,-1};
	private static final int[] SOUTH = new int[] {0,-1};
	private static final int[] SOUTHWEST = new int[] {-1,-1};
	private static final int[] WEST = new int[] {-1,0};

	
	private static final int RADIUS = 6378135;
	
	/**
	 * Returns the list of geocells (all resolutions) that are containing the point
	 * 
	 * @param point
	 * @return Returns the list of geocells (all resolutions) that are containing the point
	 */
	public static List<String> generate_geo_cell(Point point) {
		List<String> geocells = new ArrayList<String>();
		String geocellMax = Geocell.compute(point, MAX_GEOCELL_RESOLUTION);
		for(int i = 1; i < MAX_GEOCELL_RESOLUTION; i++) {
			geocells.add(Geocell.compute(point, i));
		}
		geocells.add(geocellMax);
		return geocells;
	}

	/**
	 * Returns an efficient set of geocells to search in a bounding box query.

	  This method is guaranteed to return a set of geocells having the same
	  resolution.

	 * @param bbox: A geotypes.Box indicating the bounding box being searched.
	 * @param costFunction: A function that accepts two arguments:
	        * num_cells: the number of cells to search
	        * resolution: the resolution of each cell to search
	        and returns the 'cost' of querying against this number of cells
	        at the given resolution.)
	 * @return A list of geocell strings that contain the given box.
	 */
	public static List<String> best_bbox_search_cells(BoundingBox bbox, CostFunction costFunction) {

		String cell_ne = compute(bbox.getNorthEast(), MAX_GEOCELL_RESOLUTION);
		String cell_sw = compute(bbox.getSouthWest(), MAX_GEOCELL_RESOLUTION);

		// The current lowest BBOX-search cost found; start with practical infinity.
		double min_cost = Double.MAX_VALUE;

		// The set of cells having the lowest calculated BBOX-search cost.
		List<String> min_cost_cell_set = new ArrayList<String>();

		// First find the common prefix, if there is one.. this will be the base
		// resolution.. i.e. we don't have to look at any higher resolution cells.
		int min_resolution = 0;
		int max_resoltuion = Math.min(cell_ne.length(), cell_sw.length());
		while(min_resolution < max_resoltuion  && cell_ne.substring(0, min_resolution+1).startsWith(cell_sw.substring(0, min_resolution+1))) {
			min_resolution++;
		}
		
		// Iteravely calculate all possible sets of cells that wholely contain
		// the requested bounding box.
		for(int cur_resolution = min_resolution; cur_resolution < MAX_GEOCELL_RESOLUTION + 1; cur_resolution++) {
			String cur_ne = cell_ne.substring(0, cur_resolution);
			String cur_sw = cell_sw.substring(0, cur_resolution);

			int num_cells = interpolation_count(cur_ne, cur_sw);
			if(num_cells > MAX_FEASIBLE_BBOX_SEARCH_CELLS) {
				continue;
			}

			List<String> cell_set = interpolate(cur_ne, cur_sw);
			Collections.sort(cell_set);

			// in the python version, the cost_function is given dynamically by the developer. Here i'm using the default one.
			// TODO (alex) make this function as a parameter of the method
			double cost = default_cost_function(cell_set.size(), cur_resolution);

			if(cost <= min_cost) {
				min_cost = cost;
				min_cost_cell_set = cell_set;
			} else {
				if(min_cost_cell_set.size() == 0) {
					min_cost_cell_set = cell_set;
				}
				// Once the cost starts rising, we won't be able to do better, so abort.
				break;
			}
		}
		return min_cost_cell_set;
	}
	
	/**
	 * The default cost function, used if none is provided by the developer.
	 *  
	 * @param num_cells
	 * @param resolution
	 * @return
	 */
	public static double default_cost_function(int num_cells, int resolution) {
		  return num_cells > Math.pow(GEOCELL_GRID_SIZE, 2) ? Math.exp(10000) : 0;
	}
	
	/**
	 * Determines whether the given cells are collinear along a dimension.

		  Returns True if the given cells are in the same row (column_test=False)
		  or in the same column (column_test=True).
		  
	 * @param cell1: The first geocell string.
	 * @param cell2: The second geocell string.
	 * @param column_test: A boolean, where False invokes a row collinearity test
		        and 1 invokes a column collinearity test.
	 * @return A bool indicating whether or not the given cells are collinear in the given
		    dimension.
	 */
	public static boolean collinear(String cell1, String cell2, boolean column_test) {

		for(int i = 0; i < Math.min(cell1.length(), cell2.length()); i++) {
			int l1[] = _subdiv_xy(cell1.charAt(i));
			int x1 = l1[0];
			int y1 = l1[1];
			int l2[] = _subdiv_xy(cell2.charAt(i));
			int x2 = l2[0];
			int y2 = l2[1];

			// Check row collinearity (assure y's are always the same).
			if (!column_test && y1 != y2) {
				return false;
			}

			// Check column collinearity (assure x's are always the same).
			if(column_test && x1 != x2) {
				return false;
			}
		}   
		return true;
	}
	
	/**
	 * 
	 * 	 Calculates the grid of cells formed between the two given cells.

	  Generates the set of cells in the grid created by interpolating from the
	  given Northeast geocell to the given Southwest geocell.

	  Assumes the Northeast geocell is actually Northeast of Southwest geocell.

	 * 
	 * @param cell_ne: The Northeast geocell string.
	 * @param cell_sw: The Southwest geocell string.
	 * @return A list of geocell strings in the interpolation.
	 */
	public static List<String> interpolate(String cell_ne, String cell_sw) {
		// 2D array, will later be flattened.
		LinkedList<LinkedList<String>> cell_set = new LinkedList<LinkedList<String>>();
		LinkedList<String> cell_first = new LinkedList<String>();
		cell_first.add(cell_sw);
		cell_set.add(cell_first);



		// First get adjacent geocells across until Southeast--collinearity with
		// Northeast in vertical direction (0) means we're at Southeast.
		while(!collinear(cell_first.getLast(), cell_ne, true)) {
			String cell_tmp = adjacent(cell_first.getLast(), EAST);
			if(cell_tmp == null) {
				break;
			}
			cell_first.add(cell_tmp);
		}

		// Then get adjacent geocells upwards.
		while(!cell_set.getLast().getLast().equalsIgnoreCase(cell_ne)) {
			
			LinkedList<String> cell_tmp_row = new LinkedList<String>();
			for(String g : cell_set.getLast()) {
				cell_tmp_row.add(adjacent(g, NORTH));
			}
			if(cell_tmp_row.getFirst() == null) {
				break;
			}
			cell_set.add(cell_tmp_row);
		}

		// Flatten cell_set, since it's currently a 2D array.
		List<String> result = new ArrayList<String>();
		for(LinkedList<String> list : cell_set) {
			result.addAll(list);
		}
		return result;
	}


	/**
	 * Computes the number of cells in the grid formed between two given cells.

		Computes the number of cells in the grid created by interpolating from the
		given Northeast geocell to the given Southwest geocell. Assumes the Northeast
		geocell is actually Northeast of Southwest geocell.

	 * @param cell_ne: The Northeast geocell string.
	 * @param cell_sw: The Southwest geocell string.
	 * @return An int, indicating the number of geocells in the interpolation.
	 */
	public static int interpolation_count(String cell_ne, String cell_sw) {


		BoundingBox bbox_ne = compute_box(cell_ne);
		BoundingBox bbox_sw = compute_box(cell_sw);

		double cell_lat_span = bbox_sw.getNorth() - bbox_sw.getSouth();
		double cell_lon_span = bbox_sw.getEast() - bbox_sw.getWest();

		int num_cols = (int)((bbox_ne.getEast() - bbox_sw.getWest()) / cell_lon_span);
		int num_rows = (int)((bbox_ne.getNorth() - bbox_sw.getSouth()) / cell_lat_span);

		return num_cols * num_rows;
	}

	/**
	 * 
	 * Calculates all of the given geocell's adjacent geocells.    
	 * 
	 * @param cell: The geocell string for which to calculate adjacent/neighboring cells.
	 * @return A list of 8 geocell strings and/or None values indicating adjacent cells.
	 */		
	
	public static List<String> all_adjacents(String cell) {
		List<String> result = new ArrayList<String>();
		for(int[] d : Arrays.asList(NORTHWEST, NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST)) {
			result.add(adjacent(cell, d));
		}
		return result;
	}
	
	/**
	 * Calculates the geocell adjacent to the given cell in the given direction.
	 * 
	 * @param cell: The geocell string whose neighbor is being calculated.
	 * @param dir: An (x, y) tuple indicating direction, where x and y can be -1, 0, or 1.
	        -1 corresponds to West for x and South for y, and
	         1 corresponds to East for x and North for y.
	        Available helper constants are NORTH, EAST, SOUTH, WEST,
	        NORTHEAST, NORTHWEST, SOUTHEAST, and SOUTHWEST.
	 * @return The geocell adjacent to the given cell in the given direction, or None if
	    there is no such cell.

	 */
	public static String adjacent(String cell, int[] dir) {
		if(cell == null) {
			return null;
		}
		int dx = dir[0];
		int dy = dir[1];
		char[] cell_adj_arr = cell.toCharArray(); // Split the geocell string characters into a list.
		int i = cell_adj_arr.length - 1;

		while(i >= 0 && (dx != 0 || dy != 0)) {
			int l[]= _subdiv_xy(cell_adj_arr[i]);
			int x = l[0];
			int y = l[1];

			// Horizontal adjacency.
			if(dx == -1) {  // Asking for left.
				if(x == 0) {  // At left of parent cell.
					x = GEOCELL_GRID_SIZE - 1;  // Becomes right edge of adjacent parent.
				} else {
					x--;  // Adjacent, same parent.
					dx = 0; // Done with x.
				}
			}
			else if(dx == 1) { // Asking for right.
				if(x == GEOCELL_GRID_SIZE - 1) { // At right of parent cell.
					x = 0;  // Becomes left edge of adjacent parent.
				} else {
					x++;  // Adjacent, same parent.
					dx = 0;  // Done with x.
				}
			}

			// Vertical adjacency.
			if(dy == 1) { // Asking for above.
				if(y == GEOCELL_GRID_SIZE - 1) {  // At top of parent cell.
					y = 0;  // Becomes bottom edge of adjacent parent.
				} else {
					y++;  // Adjacent, same parent.
					dy = 0;  // Done with y.
				}
			} else if(dy == -1) {  // Asking for below.
				if(y == 0) { // At bottom of parent cell.
					y = GEOCELL_GRID_SIZE - 1; // Becomes top edge of adjacent parent.
				} else {
					y--;  // Adjacent, same parent.
					dy = 0;  // Done with y.
				}
			}

			int l2[] = {x,y};
			cell_adj_arr[i] = _subdiv_char(l2);
			i--;
		}
		// If we're not done with y then it's trying to wrap vertically,
		// which is a failure.
		if(dy != 0) {
			return null;
		}

		// At this point, horizontal wrapping is done inherently.
		return new String(cell_adj_arr);
	}
	
	/**
	 * Returns whether or not the given cell contains the given point.
	 * 
	 * @param cell
	 * @param point
	 * @return Returns whether or not the given cell contains the given point.
	 */
	public static boolean contains_point(String cell, Point point) {
		return compute(point, cell.length()).equalsIgnoreCase(cell);
	}
	
	/**
	 * 	  Returns the shortest distance between a point and a geocell bounding box.

	  If the point is inside the cell, the shortest distance is always to a 'edge'
	  of the cell rectangle. If the point is outside the cell, the shortest distance
	  will be to either a 'edge' or 'corner' of the cell rectangle.
	 * 
	 * @param cell
	 * @param point
	 * @return The shortest distance from the point to the geocell's rectangle, in meters.
	 */
	public static double point_distance(String cell, Point point) {
		BoundingBox bbox = compute_box(cell);

		boolean between_w_e = bbox.getWest() <= point.getLon() && point.getLon() <= bbox.getEast();
		boolean between_n_s = bbox.getSouth() <= point.getLat() && point.getLat() <= bbox.getNorth();

		if(between_w_e) {
			if(between_n_s) {
				// Inside the geocell.
				return Math.min(
						Math.min(distance(point, new Point(bbox.getSouth(), point.getLon())),distance(point, new Point(bbox.getNorth(), point.getLon()))),
						Math.min(distance(point, new Point(point.getLat(), bbox.getEast())),distance(point, new Point(point.getLat(), bbox.getWest()))));
			} else {
				return Math.min(distance(point, new Point(bbox.getSouth(), point.getLon())),distance(point, new Point(bbox.getNorth(), point.getLon()))); 
			} 
		} else {
			if(between_n_s) {
				return Math.min(distance(point, new Point(point.getLat(), bbox.getEast())),distance(point, new Point(point.getLat(), bbox.getWest())));
			} else {
				// TODO(romannurik): optimize
				return Math.min(Math.min(distance(point, new Point(bbox.getSouth(), bbox.getEast())),distance(point, new Point(bbox.getNorth(), bbox.getEast()))),
						Math.min(distance(point, new Point(bbox.getSouth(), bbox.getWest())),distance(point, new Point(bbox.getNorth(), bbox.getWest()))));
			}
		}
	}
	/**
	 * Computes the geocell containing the given point to the given resolution.

	  This is a simple 16-tree lookup to an arbitrary depth (resolution).
	 * 
	 * @param point: The geotypes.Point to compute the cell for.
	 * @param resolution: An int indicating the resolution of the cell to compute.
	 * @return The geocell string containing the given point, of length resolution.
	 */
	public static String compute(Point point, int resolution) {
		  float north = 90.0f;
		  float south = -90.0f;
		  float east = 180.0f;
		  float west = -180.0f;

		  StringBuilder cell = new StringBuilder();
		  while(cell.length() < resolution) {
		    float subcell_lon_span = (east - west) / GEOCELL_GRID_SIZE;
		    float subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;
		    
		    int x = Math.min((int)(GEOCELL_GRID_SIZE * (point.getLon() - west) / (east - west)),
		            GEOCELL_GRID_SIZE - 1);
		    int y = Math.min((int)(GEOCELL_GRID_SIZE * (point.getLat() - south) / (north - south)),
		            GEOCELL_GRID_SIZE - 1);

		    int l[] = {x,y};
		    cell.append(_subdiv_char(l));

		    south += subcell_lat_span * y;
		    north = south + subcell_lat_span;

		    west += subcell_lon_span * x;
		    east = west + subcell_lon_span;
		  }
		  return cell.toString();
	}
	
	/**
	 * Computes the rectangular boundaries (bounding box) of the given geocell.
	 * 
	 * @param cell_: The geocell string whose boundaries are to be computed.
	 * @return A geotypes.Box corresponding to the rectangular boundaries of the geocell.
	 */
	public static BoundingBox compute_box(String cell_) {
		if(cell_ == null) {
			return null;
		}

		BoundingBox bbox = new BoundingBox(90.0, 180.0, -90.0, -180.0);
		StringBuilder cell = new StringBuilder(cell_);
		while(cell.length() > 0) {
			double subcell_lon_span = (bbox.getEast() - bbox.getWest()) / GEOCELL_GRID_SIZE;
			double subcell_lat_span = (bbox.getNorth() - bbox.getSouth()) / GEOCELL_GRID_SIZE;

			int l[] = _subdiv_xy(cell.charAt(0));
			int x = l[0];
			int y = l[1];

			bbox = new BoundingBox(bbox.getSouth() + subcell_lat_span * (y + 1),
					bbox.getWest()  + subcell_lon_span * (x + 1),
					bbox.getSouth() + subcell_lat_span * y,
					bbox.getWest()  + subcell_lon_span * x);

			cell.deleteCharAt(0);
		}

		return bbox;
	}
	
	/**
	 * Returns whether or not the given geocell string defines a valid geocell.
	 * @param cell
	 * @return Returns whether or not the given geocell string defines a valid geocell.
	 */
	public static boolean is_valid(String cell) {
		if(cell == null) {
			return false;
		}
		for(char c : cell.toCharArray()) {
			if(GEOCELL_ALPHABET.indexOf(c) < 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the (x, y) of the geocell character in the 4x4 alphabet grid.
	 * @param char_
	 * @return Returns the (x, y) of the geocell character in the 4x4 alphabet grid.
	 */
	public static int[] _subdiv_xy(char char_) {
		// NOTE: This only works for grid size 4.
		  int charI = GEOCELL_ALPHABET.indexOf(char_);
		  return new int[] {(charI & 4) >> 1 | (charI & 1) >> 0,
		          (charI & 8) >> 2 | (charI & 2) >> 1};
	}
	
	/**
	 * Returns the geocell character in the 4x4 alphabet grid at pos. (x, y).
	 * @param pos
	 * @return Returns the geocell character in the 4x4 alphabet grid at pos. (x, y).
	 */
	public static char _subdiv_char(int[] pos) {
		// NOTE: This only works for grid size 4.
		  return GEOCELL_ALPHABET.charAt(
		                 	      (pos[1] & 2) << 2 |
		                 	      (pos[0] & 2) << 1 |
		                 	      (pos[1] & 1) << 1 |
		                 	      (pos[0] & 1) << 0);
	}

	/**
	 * Calculates the great circle distance between two points (law of cosines).
	 * 
	 * @param p1: A geotypes.Point or db.GeoPt indicating the first point.
	 * @param p2: A geotypes.Point or db.GeoPt indicating the second point.
	 * @return The 2D great-circle distance between the two given points, in meters.
	 */
	public static double distance(Point p1, Point p2) {
		  double p1lat = Math.toRadians(p1.getLat());
		  double p1lon = Math.toRadians(p1.getLon());
		  double p2lat = Math.toRadians(p2.getLat());
		  double p2lon = Math.toRadians(p2.getLon());
		  return RADIUS * Math.acos(Math.sin(p1lat) * Math.sin(p2lat) +
		      Math.cos(p1lat) * Math.cos(p2lat) * Math.cos(p2lon - p1lon));
	}	    
}
