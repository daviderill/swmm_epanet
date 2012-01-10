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
