package org.example;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Debswana DrillSergeant — IT Admin PowerShell Command Centre
 * FlatLightLaf + Debswana brand colors: white, #00487B, #009BD6
 */
public class Main extends JFrame {

    // ── Debswana Brand Palette ───────────────────────────────────────────────
    static final Color BG_BASE       = Color.WHITE;
    static final Color BG_SURFACE    = new Color(245, 248, 252);
    static final Color BG_CARD       = new Color(237, 244, 251);
    static final Color BG_CARD_HOVER = new Color(220, 236, 248);
    static final Color BG_NAV        = new Color(0, 72, 123);         // #00487B

    static final Color ACCENT        = new Color(0, 155, 214);        // #009BD6
    static final Color ACCENT_DARK   = new Color(0, 72, 123);         // #00487B
    static final Color ACCENT_LIGHT  = new Color(51, 181, 229);
    static final Color ACCENT_GLOW   = new Color(0, 155, 214, 30);
    static final Color GOLD          = new Color(212, 170, 60);
    static final Color GOLD_LIGHT    = new Color(240, 200, 80);

    static final Color WARN          = new Color(220, 120, 20);
    static final Color WARN_BG       = new Color(255, 200, 100, 40);
    static final Color DANGER        = new Color(200, 50, 50);

    static final Color TEXT_H1       = new Color(15, 30, 60);
    static final Color TEXT_H2       = new Color(40, 70, 110);
    static final Color TEXT_MUTED    = new Color(120, 145, 175);
    static final Color TEXT_NAV      = new Color(200, 225, 245);      // light text on dark nav
    static final Color TEXT_ACCENT   = ACCENT_DARK;

    static final Color BORDER        = new Color(200, 215, 230);
    static final Color BORDER_ACCENT = new Color(0, 155, 214, 80);

    // ── Typography ────────────────────────────────────────────────────────────
    static final Font F_APP_TITLE = new Font("Segoe UI", Font.BOLD,   15);
    static final Font F_SECTION   = new Font("Segoe UI", Font.BOLD,   13);
    static final Font F_BODY      = new Font("Segoe UI", Font.PLAIN,  12);
    static final Font F_SMALL     = new Font("Segoe UI", Font.PLAIN,  11);
    static final Font F_BTN       = new Font("Segoe UI", Font.BOLD,   11);
    static final Font F_NAV       = new Font("Segoe UI", Font.PLAIN,  12);
    static final Font F_NAV_BOLD  = new Font("Segoe UI", Font.BOLD,   12);
    static final Font F_MONO      = new Font("Consolas", Font.PLAIN,  12);

    // ── Spacing / Geometry ────────────────────────────────────────────────────
    static final int RADIUS_CARD  = 12;
    static final int RADIUS_BTN   = 8;
    static final int RADIUS_PILL  = 20;
    static final int NAV_WIDTH    = 200;
    static final int GAP          = 8;

    // ── PowerShell admin prefix ───────────────────────────────────────────────
    private static final String ELEVATE_PREFIX =
            "if (!([Security.Principal.WindowsPrincipal]" +
            "[Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(" +
            "[Security.Principal.WindowsBuiltInRole]'Administrator')) { " +
            "Write-Host 'ERROR: Not running as Administrator.' -ForegroundColor Red; exit 1 }; ";

    // ── State ────────────────────────────────────────────────────────────────
    private JTextArea outputArea;
    private JPanel    contentStack;   // CardLayout panel swapped by nav
    private CardLayout cardLayout;
    private NavItem   activeNavLabel; // currently selected nav item

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ps-runner");
        t.setDaemon(true);
        return t;
    });

    // ════════════════════════════════════════════════════════════════════════
    public Main() {
        super("Debswana DrillSergeant");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 700));
        setPreferredSize(new Dimension(1200, 760));
        getContentPane().setBackground(BG_BASE);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        checkAdminStatus();
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(ACCENT_DARK);   // #00487B header bar
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 50, 100)),
                new EmptyBorder(10, 20, 10, 20)));

        // Left: logo + wordmark
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        ImageIcon logoIcon = loadIcon("assets/debswana-mini-logo.png", 32, 32);
        if (logoIcon != null) {
            JLabel logoImg = new JLabel(logoIcon);
            left.add(logoImg);
        } else {
            JLabel dot = new JLabel("◆");
            dot.setFont(new Font("Segoe UI", Font.BOLD, 20));
            dot.setForeground(Color.WHITE);
            left.add(dot);
        }

        JPanel wordmark = new JPanel();
        wordmark.setLayout(new BoxLayout(wordmark, BoxLayout.Y_AXIS));
        wordmark.setOpaque(false);
        JLabel appName = new JLabel("DrillSergeant");
        appName.setFont(F_APP_TITLE);
        appName.setForeground(Color.WHITE);
        JLabel appSub = new JLabel("Debswana IT Command Centre");
        appSub.setFont(F_SMALL);
        appSub.setForeground(new Color(180, 215, 240));
        wordmark.add(appName);
        wordmark.add(appSub);
        left.add(wordmark);

        // Center: pill search bar
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        searchWrap.setOpaque(false);
        RoundedTextField searchField = new RoundedTextField("🔍  Search commands…", 28, RADIUS_PILL);
        searchField.setFont(F_BODY);
        searchField.setPreferredSize(new Dimension(280, 32));
        searchWrap.add(searchField);

        // Right: clear output button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton clearBtn = makePillButton("Clear Output", new Color(0, 95, 158), new Color(200, 225, 245), new Color(0, 120, 190));
        clearBtn.addActionListener(e -> outputArea.setText(""));
        right.add(clearBtn);

        header.add(left,       BorderLayout.WEST);
        header.add(searchWrap, BorderLayout.CENTER);
        header.add(right,      BorderLayout.EAST);
        return header;
    }

    // ── Body: nav rail + content area + output console ───────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG_BASE);

        cardLayout   = new CardLayout();
        contentStack = new JPanel(cardLayout);
        contentStack.setBackground(BG_BASE);

        // Register all tab pages
        contentStack.add(wrapScroll(buildNetworkTab()),  "network");
        contentStack.add(wrapScroll(buildBootTab()),     "boot");
        contentStack.add(wrapScroll(buildUpdatesTab()),  "updates");
        contentStack.add(wrapScroll(buildDriversTab()),  "drivers");
        contentStack.add(wrapScroll(buildPriorityTab()), "quick");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildContentWithNav(), buildOutputPanel());
        split.setDividerLocation(620);
        split.setDividerSize(5);
        split.setBackground(BG_BASE);
        split.setBorder(null);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                javax.swing.plaf.basic.BasicSplitPaneDivider d = super.createDefaultDivider();
                d.setBackground(BORDER);
                return d;
            }
        });

        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildContentWithNav() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_BASE);
        panel.add(buildNavRail(),   BorderLayout.WEST);
        panel.add(contentStack,     BorderLayout.CENTER);
        return panel;
    }

    // Nav hover bg: slightly lighter than BG_NAV
    static final Color BG_NAV_HOVER  = new Color(0, 95, 158);
    // Nav active bg: #009BD6 highlight strip
    static final Color BG_NAV_ACTIVE = new Color(0, 155, 214);

    private JPanel buildNavRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(BG_NAV);
        rail.setPreferredSize(new Dimension(NAV_WIDTH, Integer.MAX_VALUE));
        rail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
                new EmptyBorder(16, 0, 16, 0)));

        JLabel sectionLabel = new JLabel("COMMANDS");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sectionLabel.setForeground(new Color(150, 195, 225));
        sectionLabel.setBorder(new EmptyBorder(0, 16, 8, 0));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rail.add(sectionLabel);

        // {iconAssetKey, label, cardKey}
        String[][] items = {
            {"wifi",        "Network",       "network"},
            {"wrench",      "Boot & Repair", "boot"},
            {"refresh-cw",  "Updates",       "updates"},
            {"monitor-cog", "Drivers",       "drivers"},
            {"pickaxe",     "Quick Run",     "quick"},
        };

        NavItem firstItem = null;
        for (String[] item : items) {
            NavItem navItem = makeNavItem(item[0], item[1], item[2]);
            if (firstItem == null) firstItem = navItem;
            rail.add(navItem);
        }

        if (firstItem != null) activateNav(firstItem, "network");

        rail.add(Box.createVerticalGlue());
        return rail;
    }

    private NavItem makeNavItem(String iconKey, String label, String cardKey) {
        // Load icon tinted white for the dark nav
        ImageIcon icon = tintIcon(loadIcon("assets/" + iconKey + ".png", 18, 18), Color.WHITE);
        NavItem item = new NavItem(icon, label);
        item.setMaximumSize(new Dimension(NAV_WIDTH, 44));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (activeNavLabel != item) { item.setNavBg(BG_NAV_HOVER); item.repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (activeNavLabel != item) { item.setNavBg(BG_NAV); item.repaint(); }
            }
            @Override public void mouseClicked(MouseEvent e) { activateNav(item, cardKey); }
        });
        return item;
    }

    private void activateNav(NavItem item, String cardKey) {
        if (activeNavLabel != null) {
            ((NavItem) activeNavLabel).setActive(false);
            ((NavItem) activeNavLabel).setNavBg(BG_NAV);
            activeNavLabel.repaint();
        }
        activeNavLabel = item;
        item.setActive(true);
        item.setNavBg(BG_NAV_ACTIVE);
        item.repaint();
        cardLayout.show(contentStack, cardKey);
    }

    /** Tints a white/grey icon to a given color by replacing non-transparent pixels. */
    private ImageIcon tintIcon(ImageIcon src, Color tint) {
        if (src == null) return null;
        int w = src.getIconWidth(), h = src.getIconHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(src.getImage(), 0, 0, null);
        g2.dispose();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = out.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 0) out.setRGB(x, y, (alpha << 24) | (tint.getRGB() & 0x00FFFFFF));
            }
        }
        return new ImageIcon(out);
    }

    private JScrollPane wrapScroll(JPanel content) {
        JScrollPane sp = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_BASE);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        return sp;
    }

    // ── Tab pages ────────────────────────────────────────────────────────────
    private JPanel buildNetworkTab() {
        JPanel p = sectionPanel("Network Connectivity", "Solve 'No Internet' or DNS errors instantly.");
        addCard(p, "wifi",        "Release & Renew IP",           "Refreshes IP address from DHCP server.",                       "ipconfig /release; ipconfig /renew",                                                                       ACCENT);
        addCard(p, "droplets",    "Flush DNS Cache",              "Clears cached DNS records that may be stale.",                  "ipconfig /flushdns",                                                                                       ACCENT);
        addCard(p, "refresh-cw",  "Reset Winsock",                "Resets the Windows network stack (requires reboot).",           "netsh winsock reset",                                                                                      ACCENT);
        addCard(p, "server",      "Reset TCP/IP Stack",           "Resets the TCP/IP protocol stack.",                            "netsh int ip reset",                                                                                       ACCENT);
        addCard(p, "monitor-cog", "Show IP Config",               "Displays all network adapter information.",                    "ipconfig /all",                                                                                            ACCENT);
        addCard(p, "shield-check","Ping Google",                  "Sends 4 packets to 8.8.8.8 to confirm internet.",              "ping 8.8.8.8 -n 4",                                                                                        ACCENT);
        return p;
    }

    private JPanel buildBootTab() {
        JPanel p = sectionPanel("Boot & System Repair", "Fix corrupted OS files causing crashes or boot failures.");
        addCard(p, "wrench",      "DISM Repair",                  "Repairs Windows component store. Requires internet (~15 min).", "DISM /Online /Cleanup-Image /RestoreHealth",                                                               WARN);
        addCard(p, "shield-check","SFC Scan",                     "Scans and repairs protected system files (~5–10 min).",         "sfc /scannow",                                                                                             WARN);
        addCard(p, "triangle-alert","Check Disk",                 "Schedules disk scan on next reboot (fixes bad sectors).",       "echo Y | chkdsk C: /f /r",                                                                                 WARN);
        addCard(p, "info",        "View System Event Errors",     "Shows the last 20 system errors from Event Log.",              "Get-EventLog -LogName System -EntryType Error -Newest 20 | Format-Table -AutoSize",                        ACCENT);
        addCard(p, "server",      "Check Disk Health (SMART)",    "Queries physical disk SMART status.",                          "Get-PhysicalDisk | Select FriendlyName,HealthStatus,OperationalStatus | Format-Table -AutoSize",            ACCENT);
        return p;
    }

    private JPanel buildUpdatesTab() {
        JPanel p = sectionPanel("Updates & Group Policy", "Fix stuck updates and force Group Policy changes.");
        addCard(p, "refresh-cw",  "Restart Windows Update",       "Stops then starts wuauserv — fixes stuck downloads.",          "net stop wuauserv; net start wuauserv",                                                                    ACCENT);
        addCard(p, "wrench",      "Update Troubleshooter",        "Launches the built-in Windows Update diagnostic wizard.",      "msdt.exe /id WindowsUpdateDiagnostic",                                                                     ACCENT);
        addCard(p, "shield-check","Force Group Policy Refresh",   "Applies all Group Policy settings immediately.",               "gpupdate /force",                                                                                          ACCENT);
        addCard(p, "info",        "Check Update Service Status",  "Shows current wuauserv status.",                               "Get-Service wuauserv | Select Name,Status,StartType",                                                      ACCENT);
        addCard(p, "download",    "List Pending Updates",         "Shows the last 10 installed hotfixes.",                        "Get-HotFix | Sort-Object InstalledOn -Descending | Select-Object -First 10 | Format-Table -AutoSize",       ACCENT);
        return p;
    }

    private JPanel buildDriversTab() {
        JPanel p = sectionPanel("Driver Management", "View, backup, or reinstall problematic drivers.");
        addCard(p, "grid-2x2",   "List 3rd-Party Drivers",       "Shows every non-Microsoft driver currently installed.",         "pnputil /enum-drivers",                                                                                    ACCENT);
        addCard(p, "folder",     "Backup All Drivers",           "Exports all drivers to C:\\DriverBackup.",                      "pnputil /export-driver * C:\\DriverBackup",                                                                WARN);
        addCard(p, "search",     "Scan for New Hardware",        "Forces Windows to detect and install new hardware.",            "pnputil /scan-devices",                                                                                    ACCENT);
        addCard(p, "triangle-alert","List Problem Devices",      "Shows devices with driver errors.",                             "Get-PnpDevice | Where-Object {$_.Status -ne 'OK'} | Format-Table -AutoSize",                              ACCENT);
        addCard(p, "info",       "Recently Installed Drivers",   "Lists drivers installed in the last 30 days.",                  "Get-WindowsDriver -Online | Sort-Object Date -Descending | Select-Object -First 15 | Format-Table -AutoSize", ACCENT);
        return p;
    }

    private JPanel buildPriorityTab() {
        JPanel p = sectionPanel("Quick Run — Recommended Order", "Run these in sequence for maximum effectiveness.");
        addCard(p, "shield-check","⚡ 1 · Force Group Policy",   "Fastest, least intrusive.",                                     "gpupdate /force",                                                                                          GOLD);
        addCard(p, "droplets",    "⚡ 2 · Flush DNS + Winsock",  "Instant network relief.",                                       "ipconfig /flushdns; netsh winsock reset",                                                                  GOLD);
        addCard(p, "wrench",      "⏱ 3 · SFC Scan",             "Medium duration (~10 min).",                                    "sfc /scannow",                                                                                             WARN);
        addCard(p, "refresh-cw",  "🌐 4 · DISM Repair",         "Long, requires internet (~15 min).",                            "DISM /Online /Cleanup-Image /RestoreHealth",                                                               WARN);
        addCard(p, "triangle-alert","🔁 5 · Check Disk",         "Longest — schedules on reboot.",                               "echo Y | chkdsk C: /f /r",                                                                                 WARN);
        return p;
    }

    // ── Output console ───────────────────────────────────────────────────────
    private JPanel buildOutputPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_SURFACE);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

        // Console header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(ACCENT_DARK);
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 50, 100)),
                new EmptyBorder(10, 16, 10, 16)));
        JLabel lbl = new JLabel("● Output Console");
        lbl.setFont(F_SECTION);
        lbl.setForeground(Color.WHITE);

        JButton copyBtn = makePillButton("Copy", new Color(0, 95, 158), new Color(200, 225, 245), new Color(0, 120, 190));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new java.awt.datatransfer.StringSelection(outputArea.getText()), null);
        });

        hdr.add(lbl,     BorderLayout.WEST);
        hdr.add(copyBtn, BorderLayout.EAST);

        outputArea = new JTextArea();
        outputArea.setFont(F_MONO);
        outputArea.setBackground(new Color(15, 35, 60));
        outputArea.setForeground(new Color(130, 210, 255));
        outputArea.setCaretColor(ACCENT_LIGHT);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        outputArea.setText("Ready. Select a command from the left panel.\n\n" +
                "⚠  Some commands require Administrator privileges.\n" +
                "   Right-click the .jar → Run as Administrator if needed.\n");

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(15, 35, 60));

        wrapper.add(hdr,    BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Footer ───────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ACCENT_DARK);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 50, 100)),
                new EmptyBorder(7, 20, 7, 20)));

        JLabel left = new JLabel("Debswana IT Department  ·  DrillSergeant v2.0");
        left.setFont(F_SMALL);
        left.setForeground(new Color(180, 215, 240));

        JLabel right = new JLabel("⚠  Run as Administrator for full functionality");
        right.setFont(F_SMALL);
        right.setForeground(new Color(255, 200, 100));

        footer.add(left,  BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── UI Helpers ───────────────────────────────────────────────────────────

    /** Creates a scrollable section panel with title + subtitle header. */
    private JPanel sectionPanel(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_BASE);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel h1 = new JLabel(title);
        h1.setFont(new Font("Segoe UI", Font.BOLD, 16));
        h1.setForeground(TEXT_H1);
        h1.setAlignmentX(LEFT_ALIGNMENT);

        JLabel h2 = new JLabel(subtitle);
        h2.setFont(F_BODY);
        h2.setForeground(TEXT_MUTED);
        h2.setAlignmentX(LEFT_ALIGNMENT);
        h2.setBorder(new EmptyBorder(3, 0, 16, 0));

        // Accent underline bar
        JPanel underline = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0,ACCENT_DARK,getWidth(),0,ACCENT);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                g2.dispose();
            }
        };
        underline.setOpaque(false);
        underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        underline.setAlignmentX(LEFT_ALIGNMENT);

        p.add(h1);
        p.add(h2);
        p.add(underline);
        p.add(Box.createVerticalStrut(14));
        return p;
    }

    /** Adds a rounded command card to a section panel. */
    private void addCard(JPanel panel, String iconKey, String label, String desc, String cmd, Color accent) {
        RoundedCard card = new RoundedCard(RADIUS_CARD, BG_CARD, BG_CARD_HOVER, accent);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Icon
        ImageIcon icon = tintIcon(loadIcon("assets/" + iconKey + ".png", 20, 20), accent);
        JLabel iconLabel = (icon != null)
                ? new JLabel(icon)
                : new JLabel("▸");
        iconLabel.setForeground(accent);
        if (icon == null) iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(28, 20));
        iconWrap.add(iconLabel);

        // Text
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(F_SECTION);
        nameLabel.setForeground(TEXT_H1);
        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(F_SMALL);
        descLabel.setForeground(TEXT_MUTED);
        text.add(nameLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(descLabel);

        // Run button
        JButton btn = new RoundedButton("▶  Run", RADIUS_BTN, accent);
        btn.setFont(F_BTN);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(90, 32));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener((ActionEvent e) -> runPowerShell(label, cmd, btn));

        card.add(iconWrap, BorderLayout.WEST);
        card.add(text,     BorderLayout.CENTER);
        card.add(btn,      BorderLayout.EAST);

        panel.add(card);
        panel.add(Box.createVerticalStrut(GAP));
    }

    /** Small pill-shaped utility button. */
    private JButton makePillButton(String text, Color bg, Color fg, Color border) {
        JButton b = new JButton(text);
        b.setFont(F_BTN);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Loads an image from resources, scales it, returns null if missing. */
    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            java.net.URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return null;
            ImageIcon raw = new ImageIcon(url);
            Image scaled = raw.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    // ── PowerShell execution ─────────────────────────────────────────────────
    private void runPowerShell(String name, String cmd, JButton btn) {
        btn.setEnabled(false);
        btn.setText("⏳");
        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        appendOutput("\n── [" + ts + "] " + name + " ──────────────────────────\n");

        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-Command", ELEVATE_PREFIX + cmd);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String fl = line;
                        SwingUtilities.invokeLater(() -> appendOutput(fl));
                    }
                }
                int code = proc.waitFor();
                String status = (code == 0) ? "✓ Done" : "✗ Exit code " + code;
                SwingUtilities.invokeLater(() -> {
                    appendOutput("\n" + status + "\n");
                    btn.setEnabled(true);
                    btn.setText("▶  Run");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendOutput("\n[ERROR] " + ex.getMessage() + "\n");
                    btn.setEnabled(true);
                    btn.setText("▶  Run");
                });
            }
        });
    }

    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void checkAdminStatus() {
        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                        "([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]" +
                        "::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]'Administrator')");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String result = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
                p.waitFor();
                boolean isAdmin = "True".equalsIgnoreCase(result != null ? result.trim() : "");
                SwingUtilities.invokeLater(() -> {
                    if (isAdmin) appendOutput("✓ Running as Administrator — all commands available.\n");
                    else appendOutput("⚠  NOT running as Administrator.\n" +
                            "   Restart as Admin for DISM, SFC, chkdsk, netsh.\n");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> appendOutput("Could not detect admin status.\n"));
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── Inner components ─────────────────────────────────────────────────────

    /** Rounded card panel with subtle border + hover glow. */
    static class RoundedCard extends JPanel {
        private final int radius;
        private final Color normalBg, hoverBg, accent;
        private boolean hovered = false;

        RoundedCard(int radius, Color normalBg, Color hoverBg, Color accent) {
            this.radius   = radius;
            this.normalBg = normalBg;
            this.hoverBg  = hoverBg;
            this.accent   = accent;
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Fill
            g2.setColor(hovered ? hoverBg : normalBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            // Border
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(hovered ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100) : BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded filled button with hover brightness. */
    static class RoundedButton extends JButton {
        private final int radius;
        private final Color base;
        private boolean hovered = false;

        RoundedButton(String text, int radius, Color base) {
            super(text);
            this.radius = radius;
            this.base   = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = hovered ? base.brighter() : base;
            if (!isEnabled()) bg = new Color(60, 70, 90);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius*2, radius*2));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded text field with placeholder. */
    static class RoundedTextField extends JTextField {
        private final String placeholder;
        private final int radius;

        RoundedTextField(String placeholder, int cols, int radius) {
            super(cols);
            this.placeholder = placeholder;
            this.radius = radius;
            setOpaque(false);
            setBorder(new EmptyBorder(4, 14, 4, 14));
            setBackground(new Color(0, 95, 158));
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(new Color(0, 120, 190));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, radius, radius));
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                g2.setColor(new Color(160, 200, 230));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, 14, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    /** Custom nav item panel with icon + label, painted with active/hover state. */
    static class NavItem extends JPanel {
        private final ImageIcon icon;
        private final String label;
        private Color navBg = BG_NAV;
        private boolean active = false;

        NavItem(ImageIcon icon, String label) {
            this.icon  = icon;
            this.label = label;
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 14, 0));
            setPreferredSize(new Dimension(NAV_WIDTH, 44));
        }

        void setNavBg(Color c)  { navBg = c; }
        void setActive(boolean a) { active = a; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Active item: left accent bar + highlight bg
            if (active) {
                g2.setColor(navBg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 8, 3, getHeight() - 16); // left accent strip
            } else {
                g2.setColor(navBg);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            // Icon
            int iconX = 16, iconY = (getHeight() - 18) / 2;
            if (icon != null) icon.paintIcon(this, g2, iconX, iconY);
            // Label
            g2.setFont(active ? F_NAV_BOLD : F_NAV);
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, iconX + 18 + 8, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Panel.background",               BG_SURFACE);
        UIManager.put("ScrollBar.trackArc",             999);
        UIManager.put("ScrollBar.thumbArc",             999);
        UIManager.put("ScrollBar.width",                8);
        UIManager.put("ScrollBar.thumb",                BORDER);
        UIManager.put("ScrollBar.track",                BG_SURFACE);
        UIManager.put("Component.focusWidth",           1);
        UIManager.put("Component.focusColor",           ACCENT);
        UIManager.put("Button.arc",                     RADIUS_BTN);
        UIManager.put("Component.arc",                  RADIUS_CARD);
        UIManager.put("TextComponent.arc",              RADIUS_PILL);
        UIManager.put("TextField.background",           Color.WHITE);
        UIManager.put("TextField.foreground",           TEXT_H1);
        UIManager.put("SplitPaneDivider.draggingColor", ACCENT_GLOW);
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
