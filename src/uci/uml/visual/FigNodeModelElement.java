// Copyright (c) 1996-98 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation for educational, research and non-profit
// purposes, without fee, and without a written agreement is hereby granted,
// provided that the above copyright notice and this paragraph appear in all
// copies. Permission to incorporate this software into commercial products
// must be negotiated with University of California. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "as is",
// without any accompanying services from The Regents. The Regents do not
// warrant that the operation of the program will be uninterrupted or
// error-free. The end-user understands that the program was developed for
// research purposes and is advised not to rely exclusively on the program for
// any reason. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY
// PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
// INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
// DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
// DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
// SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
// ENHANCEMENTS, OR MODIFICATIONS.



// File: FigNodeModelElement.java
// Classes: FigNodeModelElement
// Original Author: abonner
// $Id$

package uci.uml.visual;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.beans.*;
import com.sun.java.swing.*;
import com.sun.java.swing.plaf.metal.MetalLookAndFeel;

import uci.gef.*;
import uci.graph.*;
import uci.uml.ui.*;
import uci.uml.generate.*;
import uci.uml.Foundation.Core.*;
import uci.uml.Foundation.Data_Types.*;

/** Abstract class to display diagram icons for UML ModelElements that
 *  look like nodes and that have editiable names and can be
 *  resized. */

public abstract class FigNodeModelElement extends FigNode
implements VetoableChangeListener, DelayedVetoableChangeListener, MouseListener, KeyListener, PropertyChangeListener  {

  ////////////////////////////////////////////////////////////////
  // constants

  public static Font LABEL_FONT;
  public static Font ITALIC_LABEL_FONT;
  public final int MARGIN = 2;


  static {
    LABEL_FONT = MetalLookAndFeel.getSubTextFont();
    ITALIC_LABEL_FONT = new Font(LABEL_FONT.getFamily(),
				 Font.ITALIC, LABEL_FONT.getSize());
  }

  ////////////////////////////////////////////////////////////////
  // instance variables

  protected FigText _name;
  protected Vector _enclosedFigs = new Vector();
  protected Fig _encloser = null;
  protected boolean _readyToEdit = true;

  ////////////////////////////////////////////////////////////////
  // constructors

  public FigNodeModelElement() {
    _name = new FigText(10, 10, 90, 21);
    _name.setFont(LABEL_FONT);
    _name.setTextColor(Color.black);
    _name.setExpandOnly(true);
    _name.setMultiLine(false);
    _name.setAllowsTab(false);
    _name.setText(placeString());
    _readyToEdit = false;
  }

  /** Partially construct a new FigNode.  This method creates the
   *  _name element that holds the name of the model element and adds
   *  itself as a listener. */
  public FigNodeModelElement(GraphModel gm, Object node) {
    this();
    setOwner(node);
    _name.setText(placeString());
    _readyToEdit = false;
  }

  /** Reply text to be shown while placing node in diagram */
  public String placeString() {
    if (getOwner() instanceof ModelElement) {
      ModelElement me = (ModelElement) getOwner();
      String placeString = "new " + me.getOCLTypeStr();
      Name n = me.getName();
      if (n != null && !n.equals(Name.UNSPEC)) placeString = n.getBody();
      return placeString;
    }
    return "";
  }
  ////////////////////////////////////////////////////////////////
  // accessors

//   /** Returns true if this Fig can be resized by the user. */
//   public boolean isResizable() { return false; }
//   public boolean isLowerRightResizable() { return true; }



  public FigText getNameFig() { return _name; }

  public Vector getPopUpActions() {
    Vector popUpActions = super.getPopUpActions();
    popUpActions.addElement(new CmdUMLProperties());
    return popUpActions;
  }

  ////////////////////////////////////////////////////////////////
  // Fig API
  public Fig getEnclosingFig() { return _encloser; }

  public void setEnclosingFig(Fig f) {
    super.setEnclosingFig(f);
    if (_encloser instanceof FigNodeModelElement)
      ((FigNodeModelElement)_encloser)._enclosedFigs.removeElement(this);
    _encloser = f;
    if (_encloser instanceof FigNodeModelElement)
      ((FigNodeModelElement)_encloser)._enclosedFigs.addElement(this);
  }

  public Vector getEnclosedFigs() { return _enclosedFigs; }

  ////////////////////////////////////////////////////////////////
  // event handlers

  public void vetoableChange(PropertyChangeEvent pce) {
    //System.out.println("in vetoableChange");
    Object src = pce.getSource();
    if (src == getOwner()) {
      DelayedChangeNotify delayedNotify = new DelayedChangeNotify(this, pce);
      SwingUtilities.invokeLater(delayedNotify);
    }
    else System.out.println("FigNodeModelElement got vetoableChange"+
			    " from non-owner:" + src);
  }

  public void delayedVetoableChange(PropertyChangeEvent pce) {
    //System.out.println("in delayedVetoableChange");
    Object src = pce.getSource();
    startTrans();
    // update any text, colors, fonts, etc.
    modelChanged();
    // update the relative sizes and positions of internel Figs
    Rectangle bbox = getBounds();
    Dimension minSize = getMinimumSize();
    bbox.width = Math.max(bbox.width, minSize.width);
    bbox.height = Math.max(bbox.height, minSize.height);
    setBounds(bbox.x, bbox.y, bbox.width, bbox.height);
    endTrans();
    _readyToEdit = true;
  }

  public void propertyChange(PropertyChangeEvent pve) {
    Object src = pve.getSource();
    String pName = pve.getPropertyName();
    if (pName.equals("editing") && Boolean.FALSE.equals(pve.getNewValue())) {
      //System.out.println("finished editing");
      try {
	startTrans();
	//parse the text that was edited
	textEdited((FigText) src);
	// resize the FigNode to accomodate the new text
	Rectangle bbox = getBounds();
	Dimension minSize = getMinimumSize();
	bbox.width = Math.max(bbox.width, minSize.width);
	bbox.height = Math.max(bbox.height, minSize.height);
	setBounds(bbox.x, bbox.y, bbox.width, bbox.height);
	endTrans();
      }
      catch (PropertyVetoException ex) {
	System.out.println("could not parse and use the text you entered");
      }
    }
    else super.propertyChange(pve);
  }

  /** This method is called after the user finishes editing a text
   *  field that is in the FigNodeModelElement.  Determine which field
   *  and update the model.  This class handles the name, subclasses
   *  should override to handle other text elements. */
  protected void textEdited(FigText ft) throws PropertyVetoException {
    if (ft == _name) {
      ModelElement me = (ModelElement) getOwner();
      if (me == null) return;
      me.setName(new Name(ft.getText()));
    }
  }

  ////////////////////////////////////////////////////////////////
  // event handlers - MouseListener implementation

  /** If the user double clicks on anu part of this FigNode, pass it
   *  down to one of the internal Figs.  This allows the user to
   *  initiate direct text editing. */
  public void mouseClicked(MouseEvent me) {
    if (!_readyToEdit) {
      if (getOwner() instanceof ModelElement) {
	ModelElement own = (ModelElement) getOwner();
	try { own.setName(new Name("")); }
	catch (PropertyVetoException pve) { return; }
	_readyToEdit = true;
      }
      else {
	System.out.println("not ready to edit name");
	return;
      }
    }
    if (me.isConsumed()) return;
    if (me.getClickCount() >= 2 &&
	!(me.isPopupTrigger() || me.getModifiers() == InputEvent.BUTTON3_MASK)) {
      if (getOwner() == null) return;
      Fig f = hitFig(new Rectangle(me.getX() - 2, me.getY() - 2, 4, 4));
      if (f instanceof MouseListener) ((MouseListener)f).mouseClicked(me);
    }
    me.consume();
  }

  public void keyPressed(KeyEvent ke) {
    if (!_readyToEdit) {
      if (getOwner() instanceof ModelElement) {
	ModelElement me = (ModelElement) getOwner();
	try { me.setName(new Name("")); }
	catch (PropertyVetoException pve) { return; }
	_readyToEdit = true;
      }
      else {
	System.out.println("not ready to edit name");
	return;
      }
    }
    if (ke.isConsumed()) return;
    if (getOwner() == null) return;
    _name.keyPressed(ke);
    //ke.consume();
//     ModelElement me = (ModelElement) getOwner();
//     if (me == null) return;
//     try { me.setName(new Name(_name.getText())); }
//     catch (PropertyVetoException pve) { }
  }

  /** not used, do nothing. */
  public void keyReleased(KeyEvent ke) { }

  public void keyTyped(KeyEvent ke) {
//     if (ke.isConsumed()) return;
//     _name.keyTyped(ke);
//     //ke.consume();
//     ModelElement me = (ModelElement) getOwner();
//     if (me == null) return;
//     try { me.setName(new Name(_name.getText())); }
//     catch (PropertyVetoException pve) { }
  }

  ////////////////////////////////////////////////////////////////
  // internal methods

  /** This is called aftern any part of the UML ModelElement has
   *  changed. This method automatically updates the name FigText.
   *  Subclasses should override and update other parts. */
  protected void modelChanged() {
    ModelElement me = (ModelElement) getOwner();
    if (me == null) return;
    String nameStr = GeneratorDisplay.Generate(me.getName());
    _name.setText(nameStr);
  }


  public void setOwner(Object own) {
    Object oldOwner = getOwner();
    super.setOwner(own);
    if (oldOwner instanceof ModelElement)
      ((ModelElement)oldOwner).removeVetoableChangeListener(this);
    if (own instanceof ModelElement)
      ((ModelElement)own).addVetoableChangeListener(this);
  }


} /* end class FigNodeModelElement */
