package AtomM.GUI;

/*
 * DMessage.java
 * Класс диалогового окна для сообщений об ошибках и предупреждений
 */

public class DMessage extends javax.swing.JDialog {
    public DMessage(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    @SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jbOK = new javax.swing.JButton();
        jlText = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setName("DMessage"); // NOI18N
        setResizable(false);

        jbOK.setText("OK");
        jbOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOKActionPerformed(evt);
            }
        });

        jlText.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jlText, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(174, 174, 174)
                        .addComponent(jbOK)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jlText, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jbOK)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void setText( String text ) {
        jlText.setText( text );
    }

    private void jbOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOKActionPerformed
        dispose();
    }//GEN-LAST:event_jbOKActionPerformed

    public static void ShowMessage( String title, String text ) {
        DMessage dialog = new DMessage( new javax.swing.JFrame(), true );

        dialog.setTitle( title );
        dialog.setText( "<html>" + text + "</html>" );
        dialog.setVisible(true);
    }

    public static void ShowMessage( String text ) {
        ShowMessage( "", text );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jbOK;
    private javax.swing.JLabel jlText;
    // End of variables declaration//GEN-END:variables
}
