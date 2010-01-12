package it.yup.ui;

import java.util.Vector;

public interface UIIContainer {

	public void setSelectedItem(UIItem item);
	
	public boolean contains (UIItem item);
	
	public Vector getItems ();

}
