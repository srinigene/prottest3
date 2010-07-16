/*
 * XProtTestView.java
 * 
 * Created on Oct 2, 2009 10:15:16 PM
 * 
 */
package es.uvigo.darwin.xprottest;

import java.io.FileNotFoundException;
import es.uvigo.darwin.xprottest.compute.RunningFrame;
import es.uvigo.darwin.xprottest.compute.OptionsView;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Application;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FileDialog;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.io.File;
import java.io.PrintWriter;
import es.uvigo.darwin.xprottest.util.TextAreaAppender;
import org.jdesktop.application.Task;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.TaskService;

import es.uvigo.darwin.prottest.global.options.ApplicationOptions;
import es.uvigo.darwin.prottest.facade.ProtTestFacade;
import es.uvigo.darwin.prottest.facade.TreeFacade;
import es.uvigo.darwin.prottest.facade.TreeFacadeImpl;
import es.uvigo.darwin.prottest.model.Model;
import es.uvigo.darwin.prottest.util.ProtTestAlignment;
import es.uvigo.darwin.prottest.util.exception.AlignmentParseException;
import es.uvigo.darwin.prottest.util.exception.ProtTestInternalException;
import es.uvigo.darwin.prottest.util.factory.ProtTestFactory;
import es.uvigo.darwin.prottest.util.logging.ProtTestLogger;
import es.uvigo.darwin.prottest.util.printer.ProtTestFormattedOutput;
import es.uvigo.darwin.prottest.util.printer.ProtTestPrinter;
import java.awt.Color;
import java.io.IOException;
import org.virion.jam.util.BrowserLauncher;
import pal.alignment.Alignment;
import pal.tree.Tree;
import es.uvigo.darwin.xprottest.analysis.TreeView;
import es.uvigo.darwin.xprottest.analysis.FrequenciesView;
import es.uvigo.darwin.xprottest.analysis.consensus.Consensus;
import es.uvigo.darwin.xprottest.results.ErrorLogView;
import es.uvigo.darwin.xprottest.results.ResultsView;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * The XProtTest main frame offers whole connection between the application
 * componentes providing the full functionality of ProtTest-HPC.
 * 
 * @author Diego Darriba
 */
public class XProtTestView extends FrameView {

    /* Application colors */
    public static final Color NORMAL_COLOR = Color.BLACK;
    public static final Color CRITIC_COLOR = new Color(153, 0, 0);
    public static final Color DONE_COLOR = new Color(102, 102, 153);
    /* Settings */
    private int errorBehavior;
    /* General variables */
    private ResourceMap resourceMap;
    private Alignment alignment;
    private Tree tree;
    private ProtTestFacade prottestFacade;
    private boolean alignmentLoaded;
    private File alignmentFile;
    private boolean lnlCalculated;
    private PrintWriter displayWriter;
    private Model[] models;
    private Handler mainHandler;
    /* WINDOWS */
    private ResultsView resultsView;
    private TreeView treeView;
    private Consensus consensusView;
    private FrequenciesView frequenciesView;
    private ErrorLogView errorLogView;

    public int getErrorBehavior() {
        return errorBehavior;
    }

    protected void setErrorBehavior(int errorBehavior) {
        this.errorBehavior = errorBehavior;
    }

    public ProtTestFacade getFacade() {
        return prottestFacade;
    }

    private void setAlignmentFile(File alignmentFile) {
        enableHandler();
        int numDecimals = 5;
        alignmentLoaded = (alignmentFile != null);
        if (alignmentLoaded) {
            lblDataFileStatus.setText(resourceMap.getString("msg-data-loaded") + " " + alignmentFile.getName());
            lblDataFileStatus.setForeground(DONE_COLOR);
            this.alignmentFile = alignmentFile;
            showFrequenciesItem.setEnabled(true);
            double[] frequencies = ProtTestAlignment.getFrequencies(alignment);
            displayWriter.println("");
            displayWriter.println(resourceMap.getString("aa-frequencies"));
            displayWriter.println(
                    ProtTestFormattedOutput.space(resourceMap.getString("aa-frequencies").length(), '-'));

            for (int i = 0; i < frequencies.length; i++) {
                displayWriter.println(ProtTestAlignment.charOfIndex(i) + " - " +
                        ProtTestFormattedOutput.getDecimalString(frequencies[i], numDecimals));
            }
        } else {
            lblDataFileStatus.setText(resourceMap.getString("msg-no-data"));
            lblDataFileStatus.setForeground(CRITIC_COLOR);
            showFrequenciesItem.setEnabled(false);
        }
        frequenciesView = null;
        computeMenuItem.setEnabled(alignmentLoaded);
        resultsMenuItem.setEnabled(false);
        if (resultsView != null) {
            resultsView.dispose();
            resultsView = null;
        }
        showTreeMenuItem.setEnabled(false);
        if (treeView != null) {
            treeView.dispose();
            treeView = null;
        }
        averagingMenuItem.setEnabled(false);
        if (consensusView != null) {
            consensusView.dispose();
            consensusView = null;
        }
        disableHandler();
    }

    public PrintWriter getDisplayWriter() {
        return displayWriter;
    }

    public XProtTestView(SingleFrameApplication app, ProtTestFacade facade) {
        super(app);

        resourceMap = Application.getInstance(es.uvigo.darwin.xprottest.XProtTestApp.class).getContext().getResourceMap(XProtTestView.class);

        this.errorBehavior = Application.getInstance(es.uvigo.darwin.xprottest.XProtTestApp.class).getContext().getResourceMap(XProtTestApp.class).getInteger("default-error-behavior");

        this.prottestFacade = facade;

        initComponents();

        displayWriter = new PrintWriter(new TextAreaAppender(mainTextArea));

        mainHandler = ProtTestLogger.getDefaultLogger().addHandler(displayWriter);
        try {
            Handler logHandler = ProtTestFactory.getInstance().createLogHandler();
            if (logHandler != null) {
                ProtTestLogger.getDefaultLogger().addHandler(logHandler);
            }
        } catch (IOException ex) {
            ProtTestLogger.getDefaultLogger().severeln(ex.getMessage());
        }
        // status bar initialization - message timeout, idle icon and busy animation, etc

        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lblLikelihoodStatus.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                lblDataFileStatus.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        lblDataFileStatus.setIcon(idleIcon);

        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        lblDataFileStatus.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    lblDataFileStatus.setIcon(idleIcon);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    lblLikelihoodStatus.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                }
            }
        });
        lblMoreInfo.setVisible(false);

        ProtTestPrinter.printHeader();
        disableHandler();
    }

    private void disableHandler() {
        mainHandler.setLevel(Level.OFF);
    }
    
    private void enableHandler() {
        mainHandler.setLevel(Level.INFO);
    }
    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = XProtTestApp.getApplication().getMainFrame();
            aboutBox = new XProtTestAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        aboutBox.setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        mainScrollPane = new javax.swing.JScrollPane();
        mainTextArea = new javax.swing.JTextArea();
        statusPanel = new javax.swing.JPanel();
        lblDataFileStatus = new javax.swing.JLabel();
        lblLikelihoodStatus = new javax.swing.JLabel();
        lblMoreInfo = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        loadAlignmentMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        editCopyMenuItem = new javax.swing.JMenuItem();
        editSelectAllMenuItem = new javax.swing.JMenuItem();
        preferencesMenuItem = new javax.swing.JMenuItem();
        analysisMenu = new javax.swing.JMenu();
        computeMenuItem = new javax.swing.JMenuItem();
        showFrequenciesItem = new javax.swing.JMenuItem();
        showTreeMenuItem = new javax.swing.JMenuItem();
        averagingMenuItem = new javax.swing.JMenuItem();
        resultsMenu = new javax.swing.JMenu();
        resultsMenuItem = new javax.swing.JMenuItem();
        errorMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        manualMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(es.uvigo.darwin.xprottest.XProtTestApp.class).getContext().getResourceMap(XProtTestView.class);

        mainPanel.setSize(new java.awt.Dimension(500, 560));
        mainPanel.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(20, 20, 20, 20)));
        mainPanel.setLocation(new java.awt.Point(10, -10));
        mainPanel.setVisible(true);
        mainPanel.setAutoscrolls(true);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(null);

        //mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setName("mainPanel"); // NOI18N
        //mainPanel.setPreferredSize(new java.awt.Dimension(500, 600));

        mainScrollPane.setSize(new java.awt.Dimension(590, 600));
        mainScrollPane.setLocation(new java.awt.Point(20, 20));
        mainScrollPane.setVisible(true);
        mainScrollPane.setAutoscrolls(true);

        mainScrollPane.setName("mainScrollPane"); // NOI18N
        //mainScrollPane.setPreferredSize(new java.awt.Dimension(460, 500));

        mainTextArea.setMargin(new Insets(5, 5, 5, 5));
        mainTextArea.setBackground(Color.white);
        mainTextArea.setEditable(false);
        mainTextArea.setSize(new java.awt.Dimension(15, 10));
        mainTextArea.setAutoscrolls(true);
        mainTextArea.setVisible(true);

        //mainTextArea.setColumns(15);
        //mainTextArea.setEditable(false);
        mainTextArea.setFont(XProtTestApp.FONT_CONSOLE);
        //mainTextArea.setRows(30);
        mainTextArea.setName("mainTextArea"); // NOI18N
        //mainScrollPane.setViewportView(mainTextArea);

        statusPanel.setPreferredSize(new java.awt.Dimension(592, 30));
        statusPanel.setBorder(new javax.swing.plaf.BorderUIResource.EtchedBorderUIResource(1, new java.awt.Color(182, 182, 182), new java.awt.Color(89, 89, 89)));
        statusPanel.setLocation(new java.awt.Point(20, 630));
        statusPanel.setVisible(true);
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setForeground(java.awt.Color.blue);
        statusPanel.setBackground(new java.awt.Color(220, 220, 220));
        //statusPanel.setFont(new java.awt.Font("Dialog", 0, 9));

        //statusPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        statusPanel.setName("statusPanel"); // NOI18N
        //statusPanel.setPreferredSize(new java.awt.Dimension(100, 200));

        lblDataFileStatus.setSize(new java.awt.Dimension(150, 40));
        lblDataFileStatus.setVisible(true);
        lblDataFileStatus.setForeground(CRITIC_COLOR);
        lblDataFileStatus.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
        lblDataFileStatus.setFont(XProtTestApp.FONT_LABEL);

        //lblDataFileStatus.setForeground(resourceMap.getColor("lblDataFileStatus.foreground")); // NOI18N
        //lblDataFileStatus.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("es/uvigo/darwin/xprottest/resources/XProtTestView"); // NOI18N
        lblDataFileStatus.setText(bundle.getString("msg-no-data")); // NOI18N
        lblDataFileStatus.setName("lblDataFileStatus"); // NOI18N

        lblLikelihoodStatus.setSize(new java.awt.Dimension(270, 40));
        lblLikelihoodStatus.setVisible(true);
        lblLikelihoodStatus.setForeground(CRITIC_COLOR);
        lblLikelihoodStatus.setFont(XProtTestApp.FONT_LABEL);

        //lblLikelihoodStatus.setForeground(resourceMap.getColor("lblLikelihoodStatus.foreground")); // NOI18N
        lblLikelihoodStatus.setText(bundle.getString("msg-no-lnl-calculated")); // NOI18N
        lblLikelihoodStatus.setName("lblLikelihoodStatus"); // NOI18N

        mainPanel.add(mainScrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.PAGE_END);
        mainScrollPane.getViewport().add(mainTextArea);
        statusPanel.add(lblLikelihoodStatus, BorderLayout.LINE_START);
        statusPanel.add(lblDataFileStatus, BorderLayout.LINE_END);

        /*
        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(statusPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lblLikelihoodStatus)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(lblDataFileStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
        .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(statusPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
        .addComponent(lblLikelihoodStatus)
        .addComponent(lblDataFileStatus))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        lblMoreInfo.setForeground(resourceMap.getColor("lblMoreInfo.foreground")); // NOI18N
        lblMoreInfo.setText(resourceMap.getString("lblMoreInfo.text")); // NOI18N
        lblMoreInfo.setName("lblMoreInfo"); // NOI18N
        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
        mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(mainPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(mainPanelLayout.createSequentialGroup()
        .addGap(12, 12, 12)
        .addComponent(lblMoreInfo, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
        .addContainerGap())
        .addGroup(mainPanelLayout.createSequentialGroup()
        .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
        .addContainerGap())
        .addGroup(mainPanelLayout.createSequentialGroup()
        .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
        .addContainerGap())))
        );
        mainPanelLayout.setVerticalGroup(
        mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(lblMoreInfo)
        .addContainerGap())
        );
         */
        menuBar.setName("menuBar"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(es.uvigo.darwin.xprottest.XProtTestApp.class).getContext().getActionMap(XProtTestView.class, this);
        fileMenu.setAction(actionMap.get("openDataFile")); // NOI18N
        fileMenu.setMnemonic('F');
        fileMenu.setText(bundle.getString("item-file")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        loadAlignmentMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        loadAlignmentMenuItem.setMnemonic('L');
        loadAlignmentMenuItem.setText(bundle.getString("item-load-alignment")); // NOI18N
        loadAlignmentMenuItem.setName("loadAlignmentMenuItem"); // NOI18N
        loadAlignmentMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDataFile(evt);
            }
        });
        fileMenu.add(loadAlignmentMenuItem);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setName("menuFileExit"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText(resourceMap.getString("menu-edit")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        editCopyMenuItem.setAction(actionMap.get("editCopy")); // NOI18N
        editCopyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        editCopyMenuItem.setMnemonic('c');
        editCopyMenuItem.setText(resourceMap.getString("menu-copy")); // NOI18N
        editCopyMenuItem.setName("editCopyMenuItem"); // NOI18N
        editMenu.add(editCopyMenuItem);

        editSelectAllMenuItem.setAction(actionMap.get("editSelectAll")); // NOI18N
        editSelectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        editSelectAllMenuItem.setMnemonic('a');
        editSelectAllMenuItem.setText(resourceMap.getString("menu-selectAll")); // NOI18N
        editSelectAllMenuItem.setName("editSelectAllMenuItem"); // NOI18N
        editMenu.add(editSelectAllMenuItem);

        preferencesMenuItem.setMnemonic('p');
        preferencesMenuItem.setText(resourceMap.getString("menu-preferences")); // NOI18N
        preferencesMenuItem.setName("preferencesMenuItem"); // NOI18N
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPreferencesView(evt);
            }
        });
        editMenu.add(preferencesMenuItem);

        menuBar.add(editMenu);

        analysisMenu.setMnemonic('A');
        analysisMenu.setText(bundle.getString("item-analysis")); // NOI18N
        analysisMenu.setName("analysisMenu"); // NOI18N

        computeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        computeMenuItem.setMnemonic('c');
        computeMenuItem.setText(bundle.getString("item-compute-likelihood")); // NOI18N
        computeMenuItem.setToolTipText(resourceMap.getString("computeMenuItem.toolTipText")); // NOI18N
        computeMenuItem.setEnabled(false);
        computeMenuItem.setName("computeMenuItem"); // NOI18N
        computeMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadOptionsView(evt);
            }
        });
        analysisMenu.add(computeMenuItem);

        showFrequenciesItem.setAction(actionMap.get("showAminoacidFrequencies")); // NOI18N
        showFrequenciesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        showFrequenciesItem.setMnemonic('f');
        showFrequenciesItem.setText(bundle.getString("item-show-frequencies")); // NOI18N
        showFrequenciesItem.setToolTipText(resourceMap.getString("showFrequenciesItem.toolTipText")); // NOI18N
        showFrequenciesItem.setEnabled(false);
        showFrequenciesItem.setName("showFrequenciesItem"); // NOI18N
        analysisMenu.add(showFrequenciesItem);

        showTreeMenuItem.setAction(actionMap.get("showTree")); // NOI18N
        showTreeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        showTreeMenuItem.setMnemonic('t');
        showTreeMenuItem.setText(bundle.getString("item-show-tree")); // NOI18N
        showTreeMenuItem.setToolTipText(resourceMap.getString("showTreeMenuItem.toolTipText")); // NOI18N
        showTreeMenuItem.setEnabled(false);
        showTreeMenuItem.setName("showTreeMenuItem"); // NOI18N
        analysisMenu.add(showTreeMenuItem);

        averagingMenuItem.setAction(actionMap.get("showConsensus")); // NOI18N
        averagingMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        averagingMenuItem.setMnemonic('p');
        averagingMenuItem.setText(resourceMap.getString("averagingMenuItem.text")); // NOI18N
        averagingMenuItem.setToolTipText(resourceMap.getString("averagingMenuItem.toolTipText")); // NOI18N
        averagingMenuItem.setEnabled(false);
        averagingMenuItem.setName("averagingMenuItem"); // NOI18N
        analysisMenu.add(averagingMenuItem);

        menuBar.add(analysisMenu);

        resultsMenu.setAction(actionMap.get("showResultsWindow")); // NOI18N
        resultsMenu.setMnemonic('R');
        resultsMenu.setText(bundle.getString("item-results")); // NOI18N
        resultsMenu.setName("resultsMenu"); // NOI18N

        resultsMenuItem.setAction(actionMap.get("showResultsWindow")); // NOI18N
        resultsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        resultsMenuItem.setMnemonic('r');
        resultsMenuItem.setText(bundle.getString("item-results")); // NOI18N
        resultsMenuItem.setEnabled(false);
        resultsMenuItem.setName("resultsMenuItem"); // NOI18N
        resultsMenu.add(resultsMenuItem);

        errorMenuItem.setAction(actionMap.get("showErrorLog")); // NOI18N
        errorMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        errorMenuItem.setMnemonic('e');
        errorMenuItem.setText(resourceMap.getString("errorMenuItem.text")); // NOI18N
        errorMenuItem.setEnabled(false);
        errorMenuItem.setName("errorMenuItem"); // NOI18N
        resultsMenu.add(errorMenuItem);

        menuBar.add(resultsMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText(bundle.getString("item-help")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        manualMenuItem.setAction(actionMap.get("showManual")); // NOI18N
        manualMenuItem.setMnemonic('m');
        manualMenuItem.setText(resourceMap.getString("menu-manual")); // NOI18N
        manualMenuItem.setName("manualMenuItem"); // NOI18N
        helpMenu.add(manualMenuItem);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(bundle.getString("item-about-window")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
//        setStatusBar(statusPanel);

        this.getFrame().setLayout(new BorderLayout());
//	this.getFrame().getContentPane().add(mainPanel);

        this.getFrame().setLocation(new java.awt.Point(281, 80));
        this.getFrame().getContentPane().setLayout(new BorderLayout());
        this.getFrame().setTitle("ProtTest-HPC 3.0");
        this.getFrame().setSize(new java.awt.Dimension(630, 695));
        this.getFrame().setResizable(true);
        this.getFrame().setContentPane(mainPanel);
    }

    private void openDataFile(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDataFile
        FileDialog fc = new FileDialog(this.getFrame(), "Load DNA alignment", FileDialog.LOAD);
        fc.setDirectory(System.getProperty("user.dir"));
        fc.setVisible(true);

        String dataFileName = fc.getFile();

//        JFileChooser fc = XProtTestApp.createFileChooser(getResourceMap().getString(
//            "loadAlignment.dialogTitle"));
//        int option = fc.showOpenDialog(getFrame());
//
//        if (JFileChooser.APPROVE_OPTION == option) {
        if (dataFileName != null) {
            enableHandler();
            try {
                File f = new File(fc.getDirectory() + dataFileName);//fc.getSelectedFile();
                if (alignmentFile == null || !f.getAbsolutePath().equals(alignmentFile.getAbsolutePath())) {
                    lblLikelihoodStatus.setText(resourceMap.getString("msg-no-lnl-calculated"));
                    lblLikelihoodStatus.setForeground(CRITIC_COLOR);
                    lnlCalculated = false;
                }
                alignment = prottestFacade.readAlignment(f.getAbsolutePath(), true);
                setAlignmentFile(f);
            } catch (AlignmentParseException ex) {
                displayWriter.println(ex.getMessage());
                setAlignmentFile(null);
            } catch (FileNotFoundException ex) {
                displayWriter.println(ex.getMessage());
                setAlignmentFile(null);
            } catch (IOException ex) {
                displayWriter.println(ex.getMessage());
                setAlignmentFile(null);
            }
            disableHandler();
        }
    }//GEN-LAST:event_openDataFile

    private void loadOptionsView(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadOptionsView
        OptionsView op = new OptionsView(this,
                alignment, alignmentFile.getAbsolutePath());
        op.setVisible(true);
        this.getFrame().setEnabled(false);
    }//GEN-LAST:event_loadOptionsView

    private void loadPreferencesView(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadPreferencesView
        PreferencesView pv = new PreferencesView(this);
        pv.setVisible(true);
        this.getFrame().setEnabled(false);
    }//GEN-LAST:event_loadPreferencesView

    public void unloadOptionsView(boolean running) {
        if (!running) {
            this.getFrame().setEnabled(true);
        }
    }

    public void unloadPreferencesView() {
        this.getFrame().setEnabled(true);
    }
//    private JFileChooser createFileChooser(String title) {
//        JFileChooser fc = new JFileChooser();
//        fc.setDialogTitle(title);
//        fc.setFileFilter(fc.getAcceptAllFileFilter());
//        fc.setCurrentDirectory(new File("."));
//        return fc;
//    }
    @Action
    public void showAminoacidFrequencies() {
        if (alignment != null) {
            if (frequenciesView == null) {
                frequenciesView = new FrequenciesView(alignment);
            }
            frequenciesView.setVisible(true);
        }
    }
    private boolean correctDone;

    @Action
    public void showResultsWindow() {
        if (models != null) {
            if (resultsView == null) {
                resultsView = new ResultsView(this, alignment, models, correctDone);
            }
            resultsView.setVisible(true);
        }
    }
    private int numModels;
    private boolean taskRunning = false;

    public void computeLikelihood(int numModels, ApplicationOptions options) {

        enableHandler();
        
        preferencesMenuItem.setEnabled(false);
        this.numModels = numModels;
        if (errorLogView != null) {
            ProtTestLogger.getDefaultLogger().removeHandler(errorLogView.getLogHandler());
            errorLogView.setVisible(false);
            errorLogView.dispose();
            errorLogView = null;
        }
        if (resultsView != null) {
            resultsView.setVisible(false);
            resultsView.dispose();
            resultsView = null;
        }
        if (treeView != null) {
            treeView.setVisible(false);
            treeView.dispose();
            treeView = null;
        }
        if (consensusView != null) {
            consensusView.setVisible(false);
            consensusView.dispose();
            consensusView = null;
        }
        errorLogView = new ErrorLogView();
        ProtTestLogger.getDefaultLogger().addHandler(errorLogView.getLogHandler());
        RunningFrame runningFrame = new RunningFrame(this, numModels);
        errorMenuItem.setEnabled(false);
        prottestFacade.addObserver(runningFrame);
        lblMoreInfo.setVisible(false);

        Task task = new ComputeLikelihoodTask(getApplication(), runningFrame, this, options);
        // get the application's context...
        ApplicationContext appC = Application.getInstance().getContext();
        // ...to get the TaskMonitor and TaskService
        TaskMonitor tM = appC.getTaskMonitor();
        TaskService tS = appC.getTaskService();

        // i.e. making the animated progressbar and busy icon visible
        tS.execute(task);
        taskRunning = true;

        this.getFrame().setEnabled(false);
        runningFrame.setVisible(true);
    }

    protected void computationDone(boolean done, Model[] models) {

        preferencesMenuItem.setEnabled(true);
        if (done) {
            correctDone = (models.length == this.numModels);
            if (correctDone) {
                lblLikelihoodStatus.setText(resourceMap.getString("msg-lnl-calculated"));
                lblLikelihoodStatus.setForeground(DONE_COLOR);
                errorMenuItem.setEnabled(false);
                lblMoreInfo.setVisible(false);
                if (errorLogView != null) {
                    errorLogView.setVisible(false);
                    errorLogView.dispose();
                    errorLogView = null;
                }
            } else {
                lblLikelihoodStatus.setText("Warning! " + resourceMap.getString("models-not-complete"));
                lblLikelihoodStatus.setForeground(CRITIC_COLOR);
                displayWriter.println("");
                displayWriter.println(resourceMap.getString("models-not-complete"));
                displayWriter.println(resourceMap.getString("msg-see-error-log"));
                errorMenuItem.setEnabled(true);
                lblMoreInfo.setVisible(true);
            }
        } else {
            lblLikelihoodStatus.setText(resourceMap.getString("msg-lnl-error"));
            lblLikelihoodStatus.setForeground(CRITIC_COLOR);
            displayWriter.println(resourceMap.getString("msg-see-error-log"));
            errorMenuItem.setEnabled(true);
        }

        lnlCalculated = done;
        resultsMenuItem.setEnabled(done);
        showTreeMenuItem.setEnabled(done);
        averagingMenuItem.setEnabled(done);

        this.models = models;

        disableHandler();
    }

    public void computationInterrupted() {
        lblLikelihoodStatus.setText(resourceMap.getString("msg-lnl-error"));
        lblLikelihoodStatus.setForeground(CRITIC_COLOR);
        errorMenuItem.setEnabled(true);
        lblMoreInfo.setVisible(true);
        disableHandler();
    }

    public void unloadRunningView(RunningFrame rf) {
        this.getFrame().setEnabled(true);
        rf.setVisible(false);
        disableHandler();
    }

    private class ComputeLikelihoodTask extends Task<Object, Void> {

        private RunningFrame runningFrame;
        private XProtTestView mainFrame;
        private Model[] models;
        private ApplicationOptions options;

        ComputeLikelihoodTask(Application app,
                RunningFrame runningFrame,
                XProtTestView mainFrame,
                ApplicationOptions options) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to ComputeLikelihoodTask fields, here.
            super(app);
            this.options = options;
            this.runningFrame = runningFrame;
            this.mainFrame = mainFrame;
            runningFrame.setTask(this);
        }

        @Override
        protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
//            ProtTestPrinter printer = new ProtTestPrinter(getDisplayWriter(), new PrintWriter(System.err));

            try {
                options.setAlignment(alignmentFile.getAbsolutePath());
                models = prottestFacade.startAnalysis(options);
            } catch (AlignmentParseException ex) {
                ProtTestLogger.severeln(ex.getMessage(), this.getClass());
            } catch (IOException ex) {
                ProtTestLogger.severeln(ex.getMessage(), this.getClass());
            } catch (ProtTestInternalException ex) {
                ProtTestLogger.severeln(ex.getMessage(), this.getClass());
            }

            runningFrame.finishedExecution();

            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
            boolean done = models != null && models.length > 0;
            if (done) {
                try {
                    for (Model model : models) {
                        // throws ProtTestInternalException when Lk is not set
                        model.getLk();
                    }
                } catch (ProtTestInternalException e) {
                    done = false;
                }
            }

            mainFrame.computationDone(done, models);
        }

        @Override
        protected void cancelled() {
            mainFrame.computationDone(false, null);
        }

        @Override
        protected void interrupted(InterruptedException ex) {
            mainFrame.computationInterrupted();
        }
    }

    @Action
    public void showTree() {
        TreeFacade treeFacade = new TreeFacadeImpl();
        if (models != null) {
            if (treeView == null) {
                treeView = new TreeView(treeFacade, tree, models);
            }
            treeView.setVisible(true);
        }
    }

    @Action
    public void showConsensus() {
        TreeFacade treeFacade = new TreeFacadeImpl();
        if (models != null) {
            if (consensusView == null) {
                consensusView = new Consensus(treeFacade, models, alignment);
            }
            consensusView.setVisible(true);
        }
    }

    @Action
    public void showErrorLog() {
        if (errorLogView != null) {
            errorLogView.setVisible(true);
        }
    }

    @Action
    public void editCopy() {
        mainTextArea.copy();
    }

    @Action
    public void editSelectAll() {
        mainTextArea.selectAll();
    }

    @Action
    public void showPreferences() {
    }

    @Action
    public void showManual() {
        try {
            BrowserLauncher.openURL("http://darwin.uvigo.es/download/prottest_manual.pdf");
        } catch (IOException e) {
            displayWriter.println("Cannot open URL : " + e.getMessage());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu analysisMenu;
    private javax.swing.JMenuItem averagingMenuItem;
    private javax.swing.JMenuItem computeMenuItem;
    private javax.swing.JMenuItem editCopyMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem editSelectAllMenuItem;
    private javax.swing.JMenuItem errorMenuItem;
    private javax.swing.JLabel lblDataFileStatus;
    private javax.swing.JLabel lblLikelihoodStatus;
    private javax.swing.JLabel lblMoreInfo;
    private javax.swing.JMenuItem loadAlignmentMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane mainScrollPane;
    private javax.swing.JTextArea mainTextArea;
    private javax.swing.JMenuItem manualMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem preferencesMenuItem;
    private javax.swing.JMenu resultsMenu;
    private javax.swing.JMenuItem resultsMenuItem;
    private javax.swing.JMenuItem showFrequenciesItem;
    private javax.swing.JMenuItem showTreeMenuItem;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
