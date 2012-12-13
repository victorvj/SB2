package storyboard;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import fr.lri.swingstates.canvas.CElement;
import fr.lri.swingstates.canvas.CEllipse;
import fr.lri.swingstates.canvas.CImage;
import fr.lri.swingstates.canvas.CRectangle;
import fr.lri.swingstates.canvas.CShape;
import fr.lri.swingstates.canvas.CStateMachine;
import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.debug.StateMachineVisualization;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.JExtensionalTag;
import fr.lri.swingstates.sm.JStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Drag;
import fr.lri.swingstates.sm.transitions.Event;
import fr.lri.swingstates.sm.transitions.KeyType;
import fr.lri.swingstates.sm.transitions.Release;
import fr.lri.swingstates.sm.transitions.TimeOut;
//import fr.lri.swingstates.sm.jtransitions.*;

/**
 * The Storyboard application.
 * 
 * @author hannaschneider
 * 
 */
public class Storyboard extends JFrame {

	/**
	 * The StateMachine attached to the LayeredPanel.
	 */
	private JStateMachine sm;

	/**
	 * The state machine attached to the toolbar
	 */
	private JStateMachine toolbarStateMachine;

	/**
	 * Contains the FileTree, the Preview and the Flipchart.
	 */
	private JLayeredPane panel;

	/**
	 * The Preview for selected pictures of the FileTree.
	 */
	private ImagePanel imagePanel;

	/**
	 * The Panel with the FileTree to browse file system
	 */
	private FileTreePanel ftPanel;

	/**
	 * A copy of the preview, generated as the user starts to drag and drop the
	 * preview.
	 */
	private ImagePanel newImagePanel;

	/**
	 * Keeps track of which picture is moved.
	 */
	private int picMoved;

	/**
	 * Keeps track of which frame is selected.
	 */
	private int selectedFrame;

	/**
	 * Contains the FileTree and the Preview.
	 */
	private JPanel leftBar;

	/**
	 * The Flipchart where the Storyboard can be arranged.
	 */
	private JPanel flipchart;

	/**
	 * Toolbar panel
	 */
	private JPanel toolbarPanel;

	/**
	 * Cursor button
	 */
	private JButton cursorButton;

	/**
	 * Rectangle button
	 */
	private JButton rectangleButton;

	/**
	 * Ellipse button
	 */
	private JButton ellipseButton;

	/**
	 * Plus button
	 */
	private JButton plusButton;

	/**
	 * Minus button
	 */
	private JButton minusButton;

	/**
	 * The name of the currently selected File of the FileTree.
	 */
	private String filename;

	/**
	 * Tags the flipchart in order to determinde whether a picture is dropped in
	 * or out of the bounderies of the flipchart.
	 */
	private JExtensionalTag flipTag;

	/**
	 * Tags the preview image.
	 */
	private JExtensionalTag imageTag;

	/**
	 * An array containing the six current pictures captures. Index 0 is empty
	 * (sorry..)
	 */
	private JTextField[] captures = new JTextField[7];

	/**
	 * The borders of the frame which give feedforward for the drag and drop of
	 * images
	 */
	Canvas frames[] = new Canvas[7];

	/**
	 * The "Duplicate Buttons" between the frames
	 */
	// JButton buttons[] = new JButton[6];

	/**
	 * Constant variables defining the layout of the application.
	 */

	/**
	 * Width of an image
	 */
	static final int IMGX = 300;

	/**
	 * Height of an image
	 */
	static final int IMGY = 200;

	/**
	 * Width of the window
	 */
	static final int WINX = 1350;

	/**
	 * Height of the window
	 */
	static final int WINY = 600;

	/**
	 * Width of the left Panel with filetree and preview
	 */
	static final int LEFTX = 300;

	/**
	 * Space between pictures in the storyboard
	 */
	static final int SPACING = 35;

	/**
	 * Length and width of the "Duplicate-Buttons" between the frames
	 */
	static final int BUTTONSIZE = 15;

	boolean shapeIsSelected = false;
	int selectedShape = -1;

	/**
	 * The StateMachine, attached directly to every image, that takes care of
	 * moving an image with drag and drop inside of the flipcart.
	 */
	private JStateMachine movingStateMachine = new JStateMachine() {
		public State idling = new State() {
			public void enter() {
				System.out.println("idling");
				this.getMachine().armTimer("drag", 1000, false);
			}

			Transition press = new PressOnComponent(CStateMachine.BUTTON1,
					">> selectOrDrag") {
				// public boolean guard(){
				// return
				// !frames[whichSection(getPoint())].getDisplayList().isEmpty();
				// }
				public void action() {
					picMoved = whichSection(getPoint());
					System.out.println("go to selectOrDrag");
				}
			};

		};

		public State selectOrDrag = new State() {
			public void enter() {
				System.out.println("enter selectOrDrag");
				this.getMachine().armTimer("drag", 1000, false);
			}

			Transition dragInside = new Drag(BUTTON1,
					">> draggingInsideFlipchart") {
				public void action() {
					movePicAndCap(getPoint());
					panel.repaint();
					movingStateMachine.disarmTimer("drag");

				}
			};
			Transition timeout = new TimeOut(">> draggingInsideFlipchart");
			Transition release = new Release(CStateMachine.BUTTON1,
					">> frameSelected") {
				public void action() {
					System.out.println("selectionEvent");
					selectedFrame = whichSection(getPoint());
					fireEvent(new VirtualEvent("selectionEvent"));
					System.out.println(selectedFrame);
				}
			};

		};

		public State frameSelected = new State() {

			CRectangle rect;
			CEllipse ellip;
			Point pOrig, pEnd, pMaster;
			LinkedList<CElement> c;
			List<CShape> lista;
			CShape[] shapesList;

			// boolean shapeIsSelected = false;
			// int selectedShape = -1;

			public void enter() {
				frames[selectedFrame].setBorder(BorderFactory.createLineBorder(
						Color.yellow, 3));
				flipchart.setBackground(Color.darkGray);
				panel.requestFocus();
				rectangleButton.setEnabled(true);
				ellipseButton.setEnabled(true);
			}

			Transition deselect = new Event("deselectionEvent", ">> idling");

			Transition delete = new KeyType('d', ">> idling") {

				public boolean guard() {
					return shapeIsSelected;
				}

				public void action() {
					if (selectedShape > -1) {
						frames[selectedFrame].getDisplayList().remove(selectedShape);
						frames[selectedFrame].repaint();
						panel.repaint();
					}
					
					cursorButton.setSelected(true);
				}
			};

//			Transition pressCtrl = new PressOnComponent(CStateMachine.BUTTON1, CONTROL, ">> frameSelected") {
//				public boolean guard() {
//					System.out.println("CTROL PRESS");
//					return cursorButton.isSelected();
//				}
//			
//				public void action() {
//			
//					System.out.println("CTROL PRESS INSIDE");
//			
//					 Point mouse = (Point)this.getPoint();
//					 selectedFrame = whichSection(mouse);
//					 pOrig = realPointInSection(mouse, selectedFrame);
//					
//					 lista = frames[selectedFrame].getCanvas().getDisplayList();
//					
//					 int count = lista.size();
//					 int j = count - 1;
//					 boolean found = false;
//					 selectedShape = -1;
//					
//					 while (j > 0 && !found) {
//					
//						 CShape shape = lista.get(j);
//						 frames[selectedFrame].getCanvas().getDisplayList().get(j).setTransparencyFill(0);
//						 if ((shape.contains(pOrig) != null)
//							 && (shape.getClass().equals(CRectangle.class)
//							 || shape.getClass().equals(CEllipse.class)
//							 || shape.getClass().equals(CImage.class) )
//							 && !found) {
//							 
//							 selectedShape = j;
//							 System.out.println("press FOUNDED SHAPE " + selectedShape);
//							 found = true;
//							 frames[selectedFrame].getCanvas().getDisplayList().get(j).setTransparencyFill(0.5f);
//							 frames[selectedFrame].repaint();
//							 panel.repaint();
//						 }
//			
//						 j --;
//					 }
//				}
//			};

			Transition press = new PressOnComponent(CStateMachine.BUTTON1,
					">> frameSelected") {

				public void action() {
					if(whichSection(getPoint())==selectedFrame){

						Point mouse = (Point) this.getPoint();
						selectedFrame = whichSection(mouse);
						pOrig = realPointInSection(mouse, selectedFrame);
						
						if (cursorButton.isSelected()) {
	
							lista = frames[selectedFrame].getDisplayList();
	
							int count = lista.size();
							
							System.out.println("Content of the frame size number: " + count);
							
							int j = count - 1;
							boolean found = false;
							selectedShape = -1;
	
							while (j >= 0 && !found) {
	
								CShape shape = lista.get(j);
								setProperTransparency(shape);
								if ((shape.contains(pOrig) != null)
										&& (shape.getClass().equals(
												CRectangle.class)
												|| shape.getClass().equals(
														CEllipse.class) || shape
												.getClass().equals(CImage.class))
										&& !found) {
									selectedShape = j;
									System.out.println("press FOUNDED SHAPE "
											+ selectedShape);
									found = true;
									frames[selectedFrame].getDisplayList().get(j)
											.setTransparencyFill(0.5f);
									frames[selectedFrame].repaint();
									panel.repaint();
									
									pMaster = pOrig;
									pMaster.x = (int)frames[selectedFrame].getDisplayList()
											.get(selectedShape).getCenterX();
									pMaster.y = (int)frames[selectedFrame].getDisplayList()
											.get(selectedShape).getCenterY();
	
								}
		
								MouseEvent m = (MouseEvent) this.getInputEvent();
								int clicks = m.getClickCount();
								shapeIsSelected = (clicks == 2);
								
								if (shapeIsSelected && (selectedShape > -1)) {
									
									shape.setTransparencyFill(0.5f);
									plusButton.setEnabled(true);
									minusButton.setEnabled(true);
									frames[selectedFrame].repaint();

								}
								
								
								j--;
								
							}
	
						} else if (rectangleButton.isSelected()) {
	
							rect = new CRectangle(pOrig, 0, 0);
							rect.setOutlined(true);
							rect.setFillPaint(Color.lightGray);
							rect.setTransparencyFill(0);
							BasicStroke stroke = new BasicStroke(3);
							rect.setStroke(stroke);
						
							selectedFrame = whichSection(mouse);
							frames[selectedFrame].addShape(rect);
							frames[selectedFrame].repaint();
							panel.repaint();
	
						} else if (ellipseButton.isSelected()) {
	
							ellip = new CEllipse(pOrig, 0, 0);
							ellip.setOutlined(true);
							ellip.setFillPaint(Color.lightGray);
							ellip.setTransparencyFill(0);
							BasicStroke stroke = new BasicStroke(3);
							ellip.setStroke(stroke);
							selectedFrame = whichSection(mouse);
							frames[selectedFrame].addShape(ellip);
							frames[selectedFrame].repaint();
							panel.repaint();
	
						} 

					}

				}

			};

			Transition rotate = new DragOnComponent(CStateMachine.BUTTON1,
					CONTROL, ">> frameSelected") {
				
				boolean from1 = false;
				boolean from2 = false;
				boolean from3 = false;				
				boolean from4 = false;
				
				public boolean guard() {
					return cursorButton.isSelected();
				}

				public void action() {
					
					if (whichSection(getPoint())==selectedFrame){
	
						Point mouse = (Point) this.getPoint();
						pEnd = realPointInSection(mouse, selectedFrame);
						double angle = angleInThreePoints(pMaster, pEnd, pOrig);
												
						if ((angle >= 0) && (selectedShape > -1)) {
							frames[selectedFrame].getDisplayList()
							.get(selectedShape).rotateBy(-0.05f);
						} else {
							frames[selectedFrame].getDisplayList()
							.get(selectedShape).rotateBy(0.05f);
						}
	
						pOrig = pEnd;
						frames[selectedFrame].repaint();
					}

				}

			};

			Transition drag = new DragOnComponent(CStateMachine.BUTTON1,
					">> frameSelected") {

				public void action() {
					if(whichSection(getPoint())==selectedFrame){

						Point mouse = (Point) this.getPoint();
						pEnd = realPointInSection(mouse, selectedFrame);
	
						if (cursorButton.isSelected()) {
	
							if (selectedShape != -1) {
	
								double xH = pEnd.x - pOrig.x;
								double yH = pEnd.y - pOrig.y;
	
								lista.get(selectedShape).translateBy(xH, yH);
	
								frames[selectedFrame].repaint();
								panel.repaint();
								pOrig = pEnd;
	
							}
	
						} else if (rectangleButton.isSelected()) {
	
							rect.setDiagonal(pOrig, pEnd);
	
						} else if (ellipseButton.isSelected()) {
	
							ellip.setDiagonal(pOrig, pEnd);
	
						} 
					}

				}

			};

			Transition release = new ReleaseOnComponent(BUTTON1,
					">> frameSelected") {

				public void action() {

					if(whichSection(getPoint())==selectedFrame){

						if ((selectedShape > -1) && !shapeIsSelected) {

							setProperTransparency(frames[selectedFrame].getDisplayList()
								.get(selectedShape));
							
						}
					}
				}

			};

			public void leave() {
				resetBorders();
				flipchart.setBackground(Color.lightGray);
				cursorButton.setSelected(true);
				rectangleButton.setSelected(false);
				rectangleButton.setEnabled(false);
				ellipseButton.setSelected(false);
				ellipseButton.setEnabled(false);
			}
		};

		public State draggingInsideFlipchart = new State() {
			
        	Transition exitWindow= new Drag(CStateMachine.BUTTON1, ">> idling"){
        		public boolean guard() {
        			return ((getPoint().getX()>=WINX-30)||(getPoint().getY()<=10)||(getPoint().getY()>=WINY-30));
        		}
				public void action() {
					setPicAndCapInSection(picMoved);
					panel.repaint();
				}
        	};
			
			Transition dragOutside = new Drag(BUTTON1,
					">> draggingOutsideFlipchart") {
				public boolean guard() {
					return leftBar.contains((Point) getPoint());
				}

				public void action() {
					movePicAndCap(getPoint());
					panel.repaint();
				}
			};

			Transition dragInside = new Drag(BUTTON1) {
				public void action() {
					movePicAndCap(getPoint());
					panel.repaint();
				}
			};
			Transition release = new Release(CStateMachine.BUTTON1, ">> idling") {
				public void action() {
					int section = whichSection(getPoint());
					setPicAndCapInSection(section);
				}
			};
		};

		public State draggingOutsideFlipchart = new State() {
			
        	Transition exitWindow= new Drag(CStateMachine.BUTTON1, ">> idling"){
        		public boolean guard() {
        			return ((getPoint().getX()<=10)||(getPoint().getY()<=10)||(getPoint().getY()>=WINY-30));
        		}
				public void action() {
					setPicAndCapInSection(picMoved);
					panel.repaint();
				}
        	};
			
			Transition dragInside = new Drag(BUTTON1,
					">> draggingInsideFlipchart") {
				public boolean guard() {
					return (getPoint().getX() >= LEFTX);
				}

				public void action() {
					movePicAndCap(getPoint());
					panel.repaint();
				}
			};
			Transition dragOutside = new Drag(BUTTON1) {
				public void action() {
					movePicAndCap(getPoint());
					panel.repaint();
				}
			};
			Transition release = new Release(CStateMachine.BUTTON1, ">> idling") {
				public void action() {
					setPicAndCapInSection(picMoved);
					panel.repaint();
				}
			};
		};

	};

	/**
	 * Constructor of the Storyboard. Creates all the graphical elements: - the
	 * representation of the files and directories - the preview of selected
	 * files - the flipchart, where the user can create a storyboard by dragging
	 * and dropping pictures and adding text.
	 */
	public Storyboard() {
		setTitle("Create your Storyboard");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(WINX, WINY));
		
		//Create a panel and add components to it.
		panel = new JLayeredPane();
		panel.setBounds(0, 0, WINX, WINY);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBounds(0, 0, WINX, WINY);

		ftPanel = new FileTreePanel();
		ftPanel.setPreferredSize(new Dimension(LEFTX, WINY-IMGY));
		
		leftBar = new JPanel(new BorderLayout());
		leftBar.setBackground(Color.CYAN);
		leftBar.setPreferredSize(new Dimension(LEFTX, WINY));
		leftBar.add(ftPanel, BorderLayout.NORTH);	
		filename ="/Users/hannaschneider/Pictures/1.jpg";
		imagePanel = new ImagePanel(filename);
		imagePanel.setPreferredSize(new Dimension(IMGX,IMGY));
		leftBar.add(imagePanel, BorderLayout.SOUTH);

		imageTag = new JExtensionalTag(){};
		imageTag.addTo(imagePanel);
		imageTag.addTo(ftPanel);
		imageTag.addTo(leftBar);
		ftPanel.getTree().addTreeSelectionListener(new TreeSelectionListener(){

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				
				JTree tree = (JTree)e.getSource() ;
	            FileTreeNode node = (FileTreeNode)tree.getLastSelectedPathComponent();	
				// If the node is the leaf I can check if can open it
				if (node.isLeaf()) {
					File file = node.file;
					filename = file.getAbsolutePath();
					System.out.println(filename);
					imagePanel.setImagePath(filename);
					imagePanel.setPreferredSize(new Dimension(IMGX,IMGY));
				}
			}
		});
		
		toolbarPanel = new JPanel();
		toolbarPanel.setPreferredSize(new Dimension(SPACING, WINY));
		toolbarPanel.setLayout(new FlowLayout());
		cursorButton = new JButton();
		cursorButton.setToolTipText("Cursor");

		cursorButton.setPreferredSize(new Dimension(SPACING, SPACING));
		try {
		    Image img = ImageIO.read(getClass().getResource("SelectionTool.png"));
		    Image imgscaled = img.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH ); 
		    
		    Image imgSelected = ImageIO.read(getClass().getResource("SelectionToolSelected.png"));
		    Image imgSelectedScaled = imgSelected.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH ); 
		    
		    cursorButton.setIcon(new ImageIcon(imgscaled));
			cursorButton.setSelectedIcon(new ImageIcon(imgSelectedScaled));
			cursorButton.setPressedIcon(new ImageIcon(imgSelectedScaled));


		  } catch (IOException ex) {
		 }
		cursorButton.setSelected(true);

		rectangleButton = new JButton();
		rectangleButton.setToolTipText("Rectangle");
		rectangleButton.setPreferredSize(new Dimension(SPACING, SPACING));
		rectangleButton.setEnabled(false);
		try {
		    Image img = ImageIO.read(getClass().getResource("RectangleTool.png"));
		    Image imgscaled = img.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH );
		    
		    Image imgSelected = ImageIO.read(getClass().getResource("RectangleToolSelected.png"));
		    Image imgSelectedScaled = imgSelected.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH ); 
		    
		    rectangleButton.setIcon(new ImageIcon(imgscaled));
		    rectangleButton.setSelectedIcon(new ImageIcon(imgSelectedScaled));
		    rectangleButton.setPressedIcon(new ImageIcon(imgSelectedScaled));
		  } catch (IOException ex) {
		 }

		ellipseButton = new JButton();
		ellipseButton.setToolTipText("Ellipse");
		ellipseButton.setPreferredSize(new Dimension(SPACING, SPACING));
		ellipseButton.setEnabled(false);
		try {
		    Image img = ImageIO.read(getClass().getResource("EllipseTool.png"));
		    Image imgscaled = img.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH );
		    
		    Image imgSelected = ImageIO.read(getClass().getResource("EllipseToolSelected.png"));
		    Image imgSelectedScaled = imgSelected.getScaledInstance( SPACING-10, SPACING-10,  java.awt.Image.SCALE_SMOOTH ); 
		    
		    ellipseButton.setIcon(new ImageIcon(imgscaled));
		    ellipseButton.setSelectedIcon(new ImageIcon(imgSelectedScaled));
		    ellipseButton.setPressedIcon(new ImageIcon(imgSelectedScaled));
		    
		  } catch (IOException ex) {
		 }
		
		plusButton = new JButton("+");
		plusButton.setToolTipText("Scale up");
		plusButton.setPreferredSize(new Dimension(SPACING, SPACING));
		plusButton.setEnabled(false);
		
		minusButton = new JButton("-");
		minusButton.setToolTipText("Scale down");
		minusButton.setPreferredSize(new Dimension(SPACING, SPACING));
		minusButton.setEnabled(false);
		
		toolbarPanel.add(cursorButton);
		toolbarPanel.add(rectangleButton);
		toolbarPanel.add(ellipseButton);
		toolbarPanel.add(plusButton);
		toolbarPanel.add(minusButton);
		
		flipchart = new JPanel(new GridLayout(2,3));
		flipTag = new JExtensionalTag(){};
		flipTag.addTo(flipchart);
		flipchart.setPreferredSize(new Dimension(WINX-LEFTX-SPACING, WINY));
		flipchart.setBorder(BorderFactory.createLineBorder(Color.black));
		
		contentPane.add(leftBar, BorderLayout.LINE_START);
		contentPane.add(toolbarPanel, BorderLayout.CENTER);
		contentPane.add(flipchart, BorderLayout.LINE_END);
		panel.add(contentPane);
	
		this.add(panel);
		pack() ;
		setVisible(true) ;
		panel.requestFocus();
		
		for (int i=1;i<=6;i++){
			frames[i] = new Canvas();
			frames[i].newText(50, 250, "Hello world", new Font("verdana", Font.PLAIN, 12));
			frames[i].setBounds(0,0,IMGX+6,IMGY+6);
			frames[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
			frames[i].setLocation(getPicturePointofSection(i).x-3, getPicturePointofSection(i).y-3);
			movingStateMachine.attachTo(frames[i]);
			captures[i] = new JTextField("Capture...");
			captures[i].setFont(new Font("Comic Sans MS", Font.BOLD, 14));
			captures[i].addFocusListener(new FocusListener(){

				@Override
				public void focusGained(FocusEvent arg0) {
					JTextField field = (JTextField) arg0.getSource();
					field.selectAll();
				}

				@Override
				public void focusLost(FocusEvent arg0) {}
				
			});
			captures[i].setBounds(0,0,IMGX,30);
			captures[i].setLocation(getCapturePointofSection(i));
			panel.add(captures[i],2,0);
			
			panel.add(frames[i],2,0);
		}
		
		
		sm = new JStateMachine() {	
						
			public State idling = new State() {		
	        	
	        	Transition press = new PressOnTag(imageTag, CStateMachine.BUTTON1, ">> draggingOutsideFlipchart") {
	        		public void action(){
	        			newImagePanel = new ImagePanel(filename);
	        			newImagePanel.setBounds(0,0,IMGX,IMGY);
	        			moveNewImage(getPoint());
	            		panel.add(newImagePanel,2,0);
	            		panel.repaint();  
	        		}
	        	};	
	        	Transition select = new Event("selectionEvent", ">> selectionMode");

	        };

	        public State selectionMode = new State(){
	        	
	        	Transition deleteShape = new KeyType('d',">> selectionMode") {
	        		
	        		public boolean guard() {
	        			return shapeIsSelected;
	        		}
	        		
	                public void action() {
	                	System.out.println("deleteShape");
	                	shapeIsSelected = false;
	                	plusButton.setEnabled(false);
  					    minusButton.setEnabled(false);
	                   	if (selectedShape > -1) {
	                   		frames[selectedFrame].getDisplayList().remove(selectedShape);
	            			frames[selectedFrame].repaint();
	            			selectedShape = -1;
	               			panel.repaint(); 
	                   	}
	                }
	            };
	        	
	        	Transition delete = new KeyType('d',">> idling") {
	                
	        		public boolean guard() {
	        			return !shapeIsSelected;
	        		}
	        		
	        		public void action() {
	                	
	                	emptyFrame(selectedFrame);
		                selectedFrame = 0;
		                fireEvent(new VirtualEvent("deselectionEvent"));
		              	resetBorders();
	                	System.out.println("delete");
	                }
	            };
	            
	            Transition exit = new KeyType('e',">> idling") {
	            	public void action(){
	            		selectedFrame = 0;
	            		resetBorders();
	            		fireEvent(new VirtualEvent("deselectionEvent"));
	            	}	
	            };
	            
	        	Transition  deselect = new ClickOnComponent(CStateMachine.BUTTON1, ">> idling"){
	        		public boolean guard(){
	        			System.out.println("guard");
	        			//should return true when the cursor is on the flipchart but not on the selected frame
	        			
	        			boolean canDeselect = false;
	                	if (this.getComponent().equals(cursorButton)) {
	           						
	           				cursorButton.setSelected(true);
	           				rectangleButton.setSelected(false);
	           				ellipseButton.setSelected(false);
	           						
	           			} else if (this.getComponent().equals(rectangleButton)) {
	           						
	           				cursorButton.setSelected(false);
	      					rectangleButton.setSelected(true);
	           				ellipseButton.setSelected(false);
     						
     						if (shapeIsSelected && (selectedShape > -1)) {
	            				frames[selectedFrame].getDisplayList().get(selectedShape).setTransparencyFill(0);
	            				shapeIsSelected = false;
	            				selectedShape = -1;
	           				}
     					
	           			} else if (this.getComponent().equals(ellipseButton)) {
       						
	           				cursorButton.setSelected(false);
	           				rectangleButton.setSelected(false);
	           				ellipseButton.setSelected(true);
	           				
	           				if (shapeIsSelected && (selectedShape > -1)) {
	            				frames[selectedFrame].getDisplayList().get(selectedShape).setTransparencyFill(0);
	            				shapeIsSelected = false;
	            				selectedShape = -1;
	           				}
					
	           			} else if (this.getComponent().equals(plusButton)&&shapeIsSelected) {
       						
	           				frames[selectedFrame].getDisplayList().get(selectedShape).scaleBy(1.1f);
	  					
	  					}else if (this.getComponent().equals(minusButton)&&shapeIsSelected) {
	           			
	  						frames[selectedFrame].getDisplayList().get(selectedShape).scaleBy(0.9f);
	           				      							
						}else {
	  						
	  						canDeselect = true;
	  						
	  					}				   			
	        			
	                	boolean contains = false;
	        			if (frames[selectedFrame]!=null){
	        				contains =isInFrame(getPoint(),selectedFrame);
	        			}
	        			
	        			return !contains && canDeselect;
	        		}
	        		public void action(){
	        			fireEvent(new VirtualEvent("deselectionEvent"));
	        			System.out.println("FIRE!!");
	        		}
	        		
	        	};
	        };


	        public State draggingOutsideFlipchart = new State() {
	        	Transition dragInside = new Drag(BUTTON1, ">> draggingInsideFlipchart") {
	        		public boolean guard() {
	        			return (getPoint().getX()>=LEFTX);
	        		}
	        		public void action(){
	        			moveNewImage(getPoint());
	        			resetBorders();
	        			panel.repaint(); 
	        		}
	        	};
	        
	        	
	        	Transition dragOutside = new Drag(BUTTON1) {
	        		public void action(){
	        			moveNewImage(getPoint());
	        			panel.repaint(); 
	        		}
	        	};
	            Transition release = new Release(CStateMachine.BUTTON1, ">> idling") {
	            	public void action(){
	            		panel.remove(newImagePanel);
	            		newImagePanel = null;
	            		panel.repaint();
	            	}
	            };   
	        };
	        
	        public State draggingInsideFlipchart = new State() {	
	        	Transition dragOutside = new Drag(BUTTON1, ">> draggingOutsideFlipchart") {
	        		public boolean guard(){
	        			return leftBar.contains((Point) getPoint());
	        		}
	        		public void action(){
	        			moveNewImage(getPoint());
	        			resetBorders();
	        			panel.repaint(); 
	        		}
	        	};
	        	Transition dragInside = new Drag(BUTTON1) {
	        		public void action(){
	        			hoverFrame(whichSection(getPoint()));
	        			moveNewImage(getPoint());
	        			panel.repaint(); 
	        		}
	        	};
	            Transition release = new Release(CStateMachine.BUTTON1, ">> idling") {
	            	public void action(){
	            		int section = whichSection(getPoint());
	            		setNewImageInSection(section);  
	        		}
	            }; 
	        };	
	    };	
	    sm.attachTo(panel);
		sm.addStateMachineListener(movingStateMachine);
		movingStateMachine.addStateMachineListener(sm);
		
	}

	/**
	 * Removes all components in a given frame.
	 * 
	 * @param frame
	 *            The frame
	 */
	public void emptyFrame(int frame) {
		frames[frame].removeAllShapes();
		captures[frame].setText("Capture...");
		panel.repaint();
	}

	// /**
	// * Duplicates the content of one frame to the next frame.
	// * @param i
	// * Index of frame to duplicate
	// */
	// public void duplicate(int i) {
	// System.out.println("FRAME NUMBER " + i);
	//
	// List<CShape> lista = frames[i].getCanvas().getDisplayList();
	// frames[i+1].removeAllShapes();
	//
	// for (CShape c : lista) {
	//
	// try {
	// CShape bla = new CShape();
	// bla.cloneShape(c);
	// frames[i+1].addShape(bla);
	//
	// } catch (CloneNotSupportedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }
	// frames[i+1].repaint();
	// panel.repaint();
	// // frames[i+1].removeAllShapes();
	// // frames[i+1].getDisplayList().addAll(frames[i].getDisplayList());
	// }
	//

	// /**
	// * Disables all Duplicate-Buttons next to empty frames and enables
	// Duplicate-Buttons next to frames with content.
	// * As Duplicate-Buttons duplicate the content of one frame into the next
	// frame, they are only clickable is there is content.
	// */
	// public void updateDuplicateButtons(){
	// for(int i=1; i<=5; i++){
	// if(frames[i].getDisplayList().isEmpty()){
	// buttons[i].setEnabled(false);
	// buttons[i].setBorder(BorderFactory.createLineBorder(Color.gray,1));
	// }
	// else{
	// buttons[i].setEnabled(true);
	// }
	// }
	// }

	/**
	 * Returns the number of the section of the flipchart which contains the
	 * given point. Sections:
	 * 
	 * @param p
	 *            The point
	 * @return the number of the section
	 */
	public int whichSection(Point2D p) {
		double x = p.getX();
		double y = p.getY();
		// first row
		int flipchart = WINX - LEFTX;
		if (y <= WINY / 2) {
			if (x <= (LEFTX + (flipchart / 3))) {
				return 1;
			} else if (x <= (LEFTX + ((flipchart / 3) * 2))) {
				return 2;
			} else {
				return 3;
			}
		}
		// second row
		else {
			if (x <= (LEFTX + (flipchart / 3))) {
				return 4;
			} else if (x <= (LEFTX + ((flipchart / 3) * 2))) {
				return 5;
			} else {
				return 6;
			}
		}
	}

	/**
	 * Returns the X-coordinate for pictures placed in a given section.
	 * 
	 * @param section
	 *            The section in which the picture is placed.
	 * @return The X-value of the right position of the picture
	 */
	public int getXofSection(int section) {
		switch (section) {
		case 1:
		case 4:
			return LEFTX + SPACING + SPACING / 2;
		case 2:
		case 5:
			return LEFTX + SPACING + SPACING / 2 + IMGX + SPACING;
		case 3:
		case 6:
			return LEFTX + SPACING + SPACING / 2 + IMGX + SPACING + IMGX
					+ SPACING;
		}
		return 0;
	}

	/**
	 * Checks if the given point is in the given frame
	 * 
	 * @param point2d
	 * @param frame
	 * @return True, if the given point is in the given frame
	 */
	public boolean isInFrame(Point2D point2d, int frame) {

		switch (frame) {
		case 1:
			return (isInColumn(1, point2d) && isInRow(1, point2d));
		case 2:
			return (isInColumn(2, point2d) && isInRow(1, point2d));
		case 3:
			return (isInColumn(3, point2d) && isInRow(1, point2d));
		case 4:
			return (isInColumn(1, point2d) && isInRow(2, point2d));
		case 5:
			return (isInColumn(2, point2d) && isInRow(2, point2d));
		case 6:
			return (isInColumn(3, point2d) && isInRow(2, point2d));
		}
		return false;
	}

	/**
	 * Checks if a given point is in a given row of the storyboard (1 or 2)
	 * 
	 * @param i
	 *            The row
	 * @param point2d
	 *            The point
	 * @return True if the given point is in the given row
	 */
	public boolean isInRow(int i, Point2D point2d) {
		switch (i) {
		case 1:
			return (point2d.getY() >= SPACING && point2d.getY() <= SPACING
					+ IMGY);
		case 2:
			return (point2d.getY() >= SPACING + IMGY + SPACING + SPACING && point2d
					.getY() <= SPACING + IMGY + SPACING + SPACING + IMGY);
		}
		return false;
	}

	/**
	 * Checks if a given point is in a given column of the storyboard (1 or 2 or
	 * 3)
	 * 
	 * @param i
	 *            The column
	 * @param point2d
	 *            The point
	 * @return True if the point is in the column
	 */
	public boolean isInColumn(int i, Point2D point2d) {
		switch (i) {
		case 1:
			return (point2d.getX() >= LEFTX + SPACING && point2d.getX() <= LEFTX
					+ SPACING + IMGX);
		case 2:
			return (point2d.getX() >= LEFTX + SPACING + IMGX + SPACING && point2d
					.getX() <= LEFTX + SPACING + IMGX + SPACING + IMGX);
		case 3:
			return (point2d.getX() >= LEFTX + SPACING + IMGX + SPACING + IMGX
					+ SPACING && point2d.getX() <= LEFTX + SPACING + IMGX
					+ IMGX + SPACING + SPACING + IMGX);
		}
		return false;
	}

	/**
	 * 
	 * Returns the Y-coordinate for pictures placed in a given section.
	 * 
	 * @param section
	 *            The section in which the picture is placed.
	 * @return The Y-value of the right position of the picture
	 */
	public int getYofSection(int section) {
		switch (section) {
		case 1:
		case 2:
		case 3:
			return SPACING;
		case 4:
		case 5:
		case 6:
			return SPACING + IMGY + SPACING + SPACING;
		}
		return 0;
	}

	/**
	 * Colors the border of a frame when the mouse hovers over it dragging an
	 * image. (blue, if the hovered frame is empty and red, if a picture is
	 * placed in the hovered frame)
	 * 
	 * @param frame
	 *            Frame over which mouse hovers
	 */
	public void hoverFrame(int frame) {
		resetBorders();
		if (frames[frame].getComponentCount() == 0 && frame != picMoved) {
			frames[frame].setBorder(BorderFactory
					.createLineBorder(Color.red, 3));

		} else {

			frames[frame].setBorder(BorderFactory.createLineBorder(Color.blue,
					3));
		}
	}

	/**
	 * Resets all FrameBorders to black. The frames that contain pictures are
	 * thicker than empty frames.
	 */
	public void resetBorders() {
		for (int i = 1; i <= 6; i++) {
			if (frames[i].getComponentCount() == 0) {
				frames[i].setBorder(BorderFactory.createLineBorder(Color.black,
						3));
			} else {
				frames[i].setBorder(BorderFactory.createLineBorder(Color.black,
						1));
			}
		}
	}

	/**
	 * Returns the right position for a picture in a given section.
	 * 
	 * @param section
	 *            section for which picture coordinates are needed
	 * @return The right position for a picture in this frame as a Point
	 */
	public Point getPicturePointofSection(int section) {
		return new Point(getXofSection(section), getYofSection(section));
	}

	/**
	 * Returns the right position for a capture in a given section.
	 * 
	 * @param section
	 *            section for which capture coordinates are needed
	 * @return The right position for a capture in this frame as a Point
	 */
	public Point getCapturePointofSection(int section) {
		return new Point(getXofSection(section), getYofSection(section) + IMGY
				+ 10);
	}

	/**
	 * Moves the dragged picture to the cursor .
	 * 
	 * @param cursor
	 */
	public void movePicAndCap(Point2D cursor) {
		hoverFrame(whichSection(cursor));
		int x = (int) cursor.getX() - (IMGX / 2);
		int y = (int) cursor.getY() - (IMGY / 2);
		frames[picMoved].setLocation(x, y);
		System.out.println("try to set capture"+picMoved);
		captures[picMoved].setLocation(x, y + IMGY);
	}

	/**
	 * Moves a dragged image which has not been placed in a frame yet to the
	 * cursor.
	 * 
	 * @param cursor
	 */
	public void moveNewImage(Point2D cursor) {
		int x = (int) cursor.getX() - (IMGX / 2);
		int y = (int) cursor.getY() - (IMGY / 2);
		newImagePanel.setLocation(x, y);
	}

	/**
	 * Places the dragged image with its capture in the frame of the given
	 * section.
	 * 
	 * @param section
	 *            Number of the frame in which the moved picture is placed.
	 */
	public void setPicAndCapInSection(int section) {
		Canvas moved = frames[picMoved];
		JTextField capMoved = captures[picMoved];
		if (picMoved < section) {
			for (int i = picMoved + 1; i <= section; i++) {
				frames[i].setLocation(getPicturePointofSection(i - 1));
				captures[i].setLocation(getCapturePointofSection(i - 1));
				frames[i - 1] = frames[i];
				captures[i - 1] = captures[i];
			}
		} else {
			for (int i = picMoved - 1; i >= section; i--) {
				frames[i].setLocation(getPicturePointofSection(i + 1));
				captures[i].setLocation(getCapturePointofSection(i + 1));
				frames[i + 1] = frames[i];
				captures[i + 1] = captures[i];
			}
		}
		moved.setLocation(getPicturePointofSection(section));
		capMoved.setLocation(getCapturePointofSection(section));
		frames[section] = moved;
		captures[section] = capMoved;
		resetBorders();
		panel.repaint();
	}

	/**
	 * Returns a real point inside a selected frame
	 * 
	 * @param p
	 *            The point
	 * @param section
	 *            The selected frame number
	 * @result The point inside the selected frame
	 */
	private Point realPointInSection(Point p, int section) {

		double x = p.getX() - frames[section].getBounds().getX();
		double y = p.getY() - frames[section].getBounds().getY();

		Point result = new Point();
		result.setLocation(x, y);

		return result;

	}

	/**
	 * Transforms a point in the space given 2 points. Vectorial transformation.
	 * 
	 * @param origin
	 *            The origin point of the vector
	 * @param destination
	 *            The end point of the vector
	 * @param transform
	 *            The point to transform
	 * @return The point transformed
	 */
	private Point transformedPoint(Point origin, Point destination,
			Point transform) {

		Point result = new Point();

		int dx = destination.x - origin.x;
		int dy = destination.y - origin.y;

		double distance = Math.sqrt(dx * dx + dy * dy);

		int absDx = Math.abs(dx);
		int absDy = Math.abs(dy);

		double normVector = Math.sqrt(absDx * absDx + absDy * absDy);

		result.x = (int) (transform.x + (int) (distance * (1.0 / normVector))
				* dx);
		result.y = (int) (transform.y + (int) (distance * (1.0 / normVector))
				* dy);

		return result;
	}

	/**
	 * Set the proper transparency to a CShape based on the class
	 * @param shape
	 */
	public void setProperTransparency(CShape shape){
				
		if (shape.getClass().equals(CImage.class)) {
			shape.setTransparencyFill(1);
		} else if (shape.getClass().equals(CRectangle.class)
				|| shape.getClass().equals(CEllipse.class)) {
			shape.setTransparencyFill(0);
		}
		
	}
	
	/**
	 * Returns the angle between 3 points
	 * @param center The center point
	 * @param current The end point
	 * @param previous The origin point
	 * @return The angle in rads
	 */
	private double angleInThreePoints(Point center, Point current, Point previous) {

		  return Math.toDegrees(Math.atan2(current.x - center.x,current.y - center.y)-
		                        Math.atan2(previous.x- center.x,previous.y- center.y));
		
	}
	
	/**
	 * Places a dragged image which has never been placed in a frame before in a
	 * frame.
	 * 
	 * @param section
	 *            Section in which the image is dropped
	 */
	public void setNewImageInSection(int section) {

		panel.remove(newImagePanel);
		BufferedImage bimg = newImagePanel.getImage();
		CImage img = new CImage(filename, new Point(0, 0));
		double scale = IMGX / img.getWidth();
		System.out.println("Scale: " + scale);
		img.scaleTo(scale);
		img.setReferencePoint(0, 0);
		frames[section].addShape(img);
		resetBorders();
		System.out.println("setnewpicinsec");
		panel.repaint();
	}

	/**
	 * Visualizes the two statemachines.
	 */
	public void showMachine() {
		JFrame vizMSM = new JFrame();
		JFrame vizSM = new JFrame();
		vizMSM.getContentPane().add(
				new StateMachineVisualization(movingStateMachine));
		vizSM.getContentPane().add(new StateMachineVisualization(sm));
		vizMSM.pack();
		vizSM.pack();
		vizMSM.setVisible(true);
		vizSM.setVisible(true);

	}

	/**
	 * Gernerates a new Storyboard Window.
	 */
	public static void showWindow() {
		Storyboard sb = new Storyboard();
		sb.showMachine();

	}

	/**
	 * Starts the application.
	 * 
	 * @param args
	 */
	static public void main(String args[]) {
		showWindow();
	}
}