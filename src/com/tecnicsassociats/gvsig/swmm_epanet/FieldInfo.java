/*
 * This file is part of swmm_epanet
 * Copyright (C) 2012  Tecnics Associats
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author:
 *   David Erill <daviderill79@gmail.com>
 */

package com.tecnicsassociats.gvsig.swmm_epanet;

// Save the info of every field
// Does not save values
public class FieldInfo {

	private String DBF;
	private String Section;
	private int Position;
	private String Name;

	public FieldInfo() { }

	public FieldInfo(String dbf, String section, int position, String name) {
		this.DBF=dbf;
		this.Section=section;
		this.Position=position;
		this.Name=name;
	}
	
	public void setDBF(String dBF) {
		DBF = dBF;
	}

	public String getDBF() {
		return DBF;
	}

	public void setSection(String section) {
		Section = section;
	}

	public String getSection() {
		return Section;
	}

	public void setPosition(int position) {
		Position = position;
	}

	public int getPosition() {
		return Position;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getName() {
		return Name;
	}
	
}
