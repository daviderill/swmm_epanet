package com.tecnicsassociats.gvsig.swmm_epanet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileReader.Row;

import com.iver.andami.PluginServices;
import com.tecnicsassociats.gvsig.swmm_epanet.Constants;


public class TestExtension_SQL {

	private Properties iniProperties = new Properties();
	private static String appPath;

	private File fDbf[];
	private File fShp[];	
	private ArrayList<Map<String, String>> lMapDades;
	private File fileTemplate;
	private File fileOut;
	private Map<String, Integer> mHeader;
	private RandomAccessFile rat;
	private RandomAccessFile raf;

	private File fSqlite;	
    private Connection conn;  

    
	public TestExtension_SQL() {
		// System.out.println("TestExtension");
	}

	
	public void execute() {

		// Obtenir fitxer de properties
		if (!getPropertiesFile())
			return;

		// Connectar a BD sqlite
		if (!connectDB())
			return;

		// Comprovar existència dels fitxers
		if (!checkFiles())
			return;
		
		// Processar tots els shapes i escriure a fitxer INP
		processALL();

		// Missatge de finalització
		JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_end") + this.fileOut.getAbsoluteFile(), 
				PluginServices.getText(this, "inp_descr"), JOptionPane.PLAIN_MESSAGE); 

	}

	
	// Read content of the DBF file and saved it in an Array
	private ArrayList<Map<String, String>> readDBF(File file) {

		FileChannel in;
		Row row;
		ArrayList<Map<String, String>> mAux = null;
		Map<String, String> mDades;
		try {
			mAux = new ArrayList<Map<String, String>>();
			in = new FileInputStream(file).getChannel();
			DbaseFileReader r = new DbaseFileReader(in);
			int fields = r.getHeader().getNumFields();
			while (r.hasNext()) {
				mDades = new HashMap<String, String>();
				row = r.readRow();
				for (int i = 0; i < fields; i++) {
					String field = r.getHeader().getFieldName(i).toLowerCase();
					Object oAux = row.read(i);
					String value = oAux.toString();
					mDades.put(field, value);
				}
				mAux.add(mDades);
			}
			r.close();
		} catch (FileNotFoundException e) {
			return mAux;
		} catch (IOException e) {
			return mAux;
		}

		System.out.println("Registres processats: " + mAux.size());
		return mAux;

	}

	
	// Main procedure
	private void processALL() {
		
		try {
			// Open template and output file
			rat = new RandomAccessFile(this.fileTemplate, "r");
			raf = new RandomAccessFile(this.fileOut, "rw");
			raf.setLength(0);

			// Get content of target table	
			String sql = "SELECT id, name, dbf_id, lines FROM target";
	 		Statement stat = conn.createStatement();
	 		ResultSet rs = stat.executeQuery(sql);					
		 	while (rs.next()) {
		 		processTarget(rs.getInt("id"), rs.getInt("dbf_id"), rs.getInt("lines"));	
/*
		 		processPolygons(rs.getInt("id"), rs.getString("name").trim(), rs.getInt("dbf_id"), rs.getInt("lines"));
				mHeader = new LinkedHashMap<String, Integer>();
				mHeader.put("id_subcat", 16);
				mHeader.put("xcoord", 16);
				mHeader.put("ycoord", 16);
				mSectionHeader.put("Polygons", mHeader);
				*/
		    }		    
			rs.close();
			rat.close();
			raf.close();
 			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_io") + e.getMessage(), 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 	
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_execution") + e.getMessage(), 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 		
		}
		
	}

	
	// Process target specified by id parameter
	private void processTarget(int id, int fileIndex, int lines) throws IOException, SQLException {

		// Get data of the specified DBF file
		this.lMapDades = readDBF(fDbf[fileIndex]);

		// Go to the first line of the target
		for (int i = 1; i <= lines; i++) {
			String line = rat.readLine();
			raf.writeBytes(line + "\r\n");
		}
		
		// Get DBF fields to write into this target
		mHeader = new LinkedHashMap<String, Integer>();		
		String sql = "SELECT name, space FROM target_fields WHERE target_id = " + id + " ORDER BY pos" ;
 		Statement stat = conn.createStatement();
 		ResultSet rs = stat.executeQuery(sql);			 		
		while (rs.next()) {
			mHeader.put(rs.getString("name").trim(), rs.getInt("space"));
		}
		rs.close();
		
		ListIterator<Map<String, String>> it = this.lMapDades.listIterator();
		Map<String, String> m;
		// Iterate over DBF content
		while (it.hasNext()) {
			m = it.next();
			Set<String> set = mHeader.keySet();
			Iterator<String> itKey = set.iterator();
			// Iterate over fields to write
			while (itKey.hasNext()) {
				String sKey = (String) itKey.next();
				sKey = sKey.toLowerCase();
				int size = mHeader.get(sKey);
				// Write to the output file if the key exists
				if (m.containsKey(sKey)) {
					String sValor = (String) m.get(sKey);
					raf.writeBytes(sValor);
					// Complete spaces with empty values
					for (int j = sValor.length(); j <= size; j++) {
						raf.writeBytes(" ");
					}
				}
				// If key doesn't exist write empty spaces
				else{
					for (int j = 0; j <= size; j++) {
						raf.writeBytes(" ");
					}					
				}
			}
			raf.writeBytes("\r\n");
		}
		
	}

	
	// Get Properties File
	private boolean getPropertiesFile() {
		
		appPath = System.getProperty("user.dir");
		// appPath = PluginServices.getPluginServices(this).getPluginDirectory().getPath();
		String sFile = appPath + File.separator + "config" + File.separator + Constants.INI_FILE;		
		File fileIni = new File(sFile);
		try {
			iniProperties.load(new FileInputStream(fileIni));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_notfound") + sFile, 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 			
			return false;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_io") + sFile, 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 	
			return false;
		}
		return !iniProperties.isEmpty();
		
	}
	
	
	// Check all the necessary files to run the process
	private boolean checkFiles() {

		// Get shape folder and output inp
		String sDirInp;		
		String sFile;
		String sDirShp;				
		sDirInp = iniProperties.getProperty("DIR_INP");
		sDirInp = appPath + File.separator + sDirInp;
		sDirShp = iniProperties.getProperty("DIR_SHP");
		sDirShp = appPath + File.separator + sDirShp;

		// Get INP template file
		sFile = iniProperties.getProperty("INP_TEMPLATE");
		sFile = sDirInp + File.separator + sFile;
		fileTemplate = new File(sFile);
		if (!fileTemplate.exists()) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_notfound") + sFile, 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 				
			return false;
		}

		// Get INP output file
		sFile = iniProperties.getProperty("INP_OUT");
		sFile = sDirInp + File.separator + sFile;
		fileOut = new File(sFile);

		// Get from Database Shapes and DBF's to handle
		String sTotal = iniProperties.getProperty("DBF_TOTAL");
		int total = Integer.parseInt(sTotal.trim());
		fDbf = new File[total];
		fShp = new File[total];	
		boolean ok = true;
		String sql = "SELECT id, name FROM dbf ORDER BY id";
	 	try {
	 		Statement stat = conn.createStatement();
	 		ResultSet rs = stat.executeQuery(sql);		
			while (rs.next() && ok) {
				checkFile(sDirShp, rs.getString("name").trim(), rs.getInt("id"));
			}
			rs.close();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_execution") + e.getMessage(), 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 						
		    return false;	
		}				
		
		return ok;

	}

	
	// Check if DBF and Shapefile exist
	public boolean checkFile(String sDir, String sFile, int index) {
		
		String sDBF = sDir + File.separator + sFile + ".dbf";
		fDbf[index] = new File(sDBF);
		if (!fDbf[index].exists()) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_notfound") + sFile, 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 		
			return false;
		}
		String sSHP = sDir + File.separator + sFile + ".shp";
		fShp[index] = new File(sSHP);
		if (!fShp[index].exists()) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_notfound") + sFile, 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE);
			return false;
		}		
		return true;
		
	}
	
	
	// Connect to sqlite Database
	private boolean connectDB(){
		try {
			
		    Class t = Class.forName("org.sqlite.JDBC");
		    
			// Get INP output file
			String sDirInp = iniProperties.getProperty("DIR_INP");
			sDirInp = appPath + File.separator + sDirInp;		    
		    String sFile = iniProperties.getProperty("INP_DB");
		    sFile = sDirInp + File.separator + sFile;
			
			fSqlite = new File(sFile);
			if (fSqlite.exists()) {
			    //sqliteURL = this.getClass().getClassLoader().getResource("inp.sqlite");
				conn = DriverManager.getConnection("jdbc:sqlite:" + sFile);
			    return true;				
			}
			else{
				JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_notfound") + sFile, 
						PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 						
				return false;
			}
		    /*
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://geoserver4:5432/gdb_mcnb";
		 	Connection conn = DriverManager.getConnection(url, "user_mcnb", "mcnb");
		 	*/	
		} catch(SQLException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_connection") + e.getMessage(), 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 						
		    return false;			
		} catch(ClassNotFoundException e) {
			JOptionPane.showMessageDialog(null, PluginServices.getText(this, "inp_error_connection") + "ClassNotFoundException", 
					PluginServices.getText(this, "inp_descr"), JOptionPane.ERROR_MESSAGE); 					
		    return false;
		}	
		
    }
	
	
}
