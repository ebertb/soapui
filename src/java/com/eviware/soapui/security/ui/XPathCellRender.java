/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */
package com.eviware.soapui.security.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;

@SuppressWarnings( "serial" )
public class XPathCellRender extends AbstractCellEditor implements TableCellEditor, MouseListener, WindowFocusListener
{

	protected JPanel panel;
	JTextArea textArea;

	protected static final String EDIT = "edit";

	private JTextField textField;
	protected JFrame frame;
	private JButton resizeBtn;
	protected int mouseX;
	protected int mouseY;

	public XPathCellRender()
	{

		panel = new JPanel();
		panel.setLayout( new BorderLayout() );
		textArea = new JTextArea( 4, 5 );
		textArea.setWrapStyleWord( true );

		textArea.addKeyListener( new KeyAdapter()
		{
			public void keyPressed( KeyEvent evt )
			{
				switch( evt.getKeyCode() )
				{
				case KeyEvent.VK_ENTER :
					textField.setText( textArea.getText() );
					frame.setVisible( false );
					break;
				case KeyEvent.VK_ESCAPE :
					frame.setVisible( false );
					break;
				}
			}
		} );

		panel.add( new JScrollPane( textArea ), BorderLayout.CENTER );
		panel.setPreferredSize( new Dimension( 200, 100 ) );
		panel.setMinimumSize( new Dimension( 200, 100 ) );
		JXToolBar toolbar = initToolbar(UISupport.createToolbar());
		panel.add( toolbar, BorderLayout.SOUTH );

		this.frame = new JFrame();
		this.frame.addWindowFocusListener( this );
		this.frame.setContentPane( panel );
		this.frame.setAlwaysOnTop( true );
		this.frame.setModalExclusionType( ModalExclusionType.APPLICATION_EXCLUDE );
		this.frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.frame.setUndecorated( true );
		textField = new JTextField();
		textField.addMouseListener( this );

	}

	/**
	 * @param jxToolBar 
	 * @return
	 */
	protected JXToolBar initToolbar(JXToolBar toolbar)
	{

		resizeBtn = UISupport.createToolbarButton( UISupport.createImageIcon( "/icon_resize.gif" ) );
		resizeBtn.setCursor( new Cursor(Cursor.SE_RESIZE_CURSOR) );
		resizeBtn.setContentAreaFilled( false );
		resizeBtn.setBorder( null );
		resizeBtn.addMouseMotionListener( new MouseMotionListener()
		{
			
			@Override
			public void mouseMoved( MouseEvent e )
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseDragged( MouseEvent e )
			{
				frame.setSize(frame.getWidth() - mouseX + e.getX(), frame.getHeight() - mouseY + e.getY());
			}
		});
		
		resizeBtn.addMouseListener( new MouseAdapter()
		{
			
			public void mousePressed(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
			};
			
		} );
		toolbar.addGlue();
		toolbar.add( resizeBtn );
		return toolbar;
	}

	@Override
	public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column )
	{
		String val = ( String )table.getModel().getValueAt( row, column );
		textField.setText( val );
		return textField;
	}

	@Override
	public Object getCellEditorValue()
	{
		return textField.getText();
	}

	@Override
	public void mouseClicked( MouseEvent e )
	{
		if( !frame.isVisible() )
		{
			textArea.setText( textField.getText() );
			Point position = textField.getLocationOnScreen();
			frame.setBounds( position.x, position.y, frame.getWidth(), frame.getHeight() );
			frame.pack();
			frame.setVisible( true );
		}
	}

	@Override
	public void mouseEntered( MouseEvent e )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited( MouseEvent e )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed( MouseEvent e )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased( MouseEvent e )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void windowGainedFocus( WindowEvent e )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void windowLostFocus( WindowEvent e )
	{
		frame.setVisible( false );
	}

}