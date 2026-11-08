import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Práctica Tema 1: Programación Multiproceso - 2º DAW
 * Buscador de Archivos por Extensión y Lanzador Asíncrono de Procesos (.exe)
 * Compatible con Java 17, 21 y 25.
 */
public class BuscadorProcesosApp extends JFrame {

    private JTextField txtExtension;
    private JButton btnBuscar;
    private JButton btnCancelar;
    private DefaultListModel<String> listModel;
    private JList<String> listResultados;
    private JProgressBar progressBar;
    private JLabel lblEstado;
    private SwingWorker<Void, String> searchWorker;

    public BuscadorProcesosApp() {
        super("Buscador de Archivos y Lanzador de Procesos - 2º DAW");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);

        // Panel principal con BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Panel Superior / Central: Listado con Scroll
        listModel = new DefaultListModel<>();
        listResultados = new JList<>(listModel);
        listResultados.setFont(new Font("Consolas", Font.PLAIN, 12));
        listResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listResultados.setToolTipText("Haga doble clic sobre un archivo ejecutable (.exe) para lanzarlo");

        // Listener de Doble Clic para lanzar proceso .exe
        listResultados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = listResultados.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String rutaSeleccionada = listModel.getElementAt(index);
                        ejecutarFichero(rutaSeleccionada);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(listResultados);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Archivos Encontrados (Doble clic en un .exe para ejecutar)"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 2. Panel Inferior: Controles de Búsqueda y Barra de Estado
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));

        // Panel de entrada
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel lblExt = new JLabel("Extensión del archivo:");
        lblExt.setFont(new Font("SansSerif", Font.BOLD, 12));
        txtExtension = new JTextField(".exe", 10);
        txtExtension.setFont(new Font("SansSerif", Font.PLAIN, 12));

        btnBuscar = new JButton("Buscar");
        btnBuscar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnBuscar.setBackground(new Color(33, 115, 70));
        btnBuscar.setForeground(Color.WHITE);

        btnCancelar = new JButton("Detener");
        btnCancelar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancelar.setEnabled(false);

        btnBuscar.addActionListener(e -> iniciarBusqueda());
        btnCancelar.addActionListener(e -> detenerBusqueda());
        txtExtension.addActionListener(e -> iniciarBusqueda());

        inputPanel.add(lblExt);
        inputPanel.add(txtExtension);
        inputPanel.add(btnBuscar);
        inputPanel.add(btnCancelar);

        // Panel de Estado y Progreso
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        lblEstado = new JLabel("Listo. Ingrese una extensión (ej: .exe o .dll) y pulse 'Buscar'.");
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 11));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusPanel.add(lblEstado, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void iniciarBusqueda() {
        String ext = txtExtension.getText().trim();
        if (ext.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, indique una extensión a buscar (ej: .exe)", "Campo Requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        final String extensionBuscada = ext.toLowerCase();

        listModel.clear();
        btnBuscar.setEnabled(false);
        btnCancelar.setEnabled(true);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        lblEstado.setText("Buscando ficheros '" + extensionBuscada + "' en todas las unidades del equipo...");

        searchWorker = new SwingWorker<Void, String>() {
            private int contador = 0;

            @Override
            protected Void doInBackground() {
                File[] unidades = File.listRoots();
                if (unidades != null) {
                    for (File unidad : unidades) {
                        if (isCancelled()) break;
                        escanearDirectorio(unidad, extensionBuscada);
                    }
                }
                return null;
            }

            private void escanearDirectorio(File dir, String extension) {
                if (isCancelled() || dir == null) return;

                File[] archivos = null;
                try {
                    archivos = dir.listFiles();
                } catch (SecurityException se) {
                    // Ignorar carpetas del sistema sin permisos de acceso
                    return;
                }

                if (archivos != null) {
                    for (File f : archivos) {
                        if (isCancelled()) break;
                        if (f.isDirectory()) {
                            // Evitar carpetas problemáticas de Windows o enlaces simbólicos recursivos
                            String nombre = f.getName().toLowerCase();
                            if (!nombre.equals("$recycle.bin") && !nombre.equals("system volume information")) {
                                escanearDirectorio(f, extension);
                            }
                        } else if (f.isFile()) {
                            if (f.getName().toLowerCase().endsWith(extension)) {
                                contador++;
                                publish(f.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String ruta : chunks) {
                    listModel.addElement(ruta);
                }
                lblEstado.setText("Buscando... Encontrados: " + contador + " archivos.");
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                btnCancelar.setEnabled(false);
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);

                if (isCancelled()) {
                    lblEstado.setText("Búsqueda detenida por el usuario. Archivos listados: " + listModel.size());
                } else {
                    lblEstado.setText("Búsqueda finalizada con éxito. Total archivos encontrados: " + listModel.size());
                    if (listModel.isEmpty()) {
                        JOptionPane.showMessageDialog(BuscadorProcesosApp.this,
                                "No se encontró ningún archivo con la extensión " + extensionBuscada,
                                "Sin Resultados", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };

        searchWorker.execute();
    }

    private void detenerBusqueda() {
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
            btnCancelar.setEnabled(false);
            lblEstado.setText("Cancelando búsqueda...");
        }
    }

    private void ejecutarFichero(String rutaFichero) {
        if (rutaFichero == null || rutaFichero.trim().isEmpty()) return;

        File archivo = new File(rutaFichero);
        if (!archivo.exists()) {
            JOptionPane.showMessageDialog(this,
                    "El archivo seleccionado ya no existe en el sistema:\n" + rutaFichero,
                    "Archivo no encontrado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Requisito del enunciado: solamente se admiten ejecutables .exe
        if (!archivo.getName().toLowerCase().endsWith(".exe")) {
            JOptionPane.showMessageDialog(this,
                    "Acción no permitida:\nSolo se admite la ejecución directa de programas ejecutables (.exe).\n\nArchivo seleccionado: " + archivo.getName(),
                    "Tipo de Archivo No Ejecutable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Desea iniciar el siguiente proceso del sistema operativo?\n\nRuta: " + rutaFichero,
                "Confirmación de Ejecución de Proceso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Creación y ejecución de un nuevo proceso del Sistema Operativo con ProcessBuilder
                ProcessBuilder pb = new ProcessBuilder(rutaFichero);
                pb.directory(archivo.getParentFile());
                Process proceso = pb.start();

                // Obtención del PID mediante la API estándar de Java (Process.pid())
                long pid = proceso.pid();

                JOptionPane.showMessageDialog(this,
                        "¡Proceso iniciado satisfactoriamente en el Sistema Operativo!\n\n" +
                                "Nombre del Ejecutable: " + archivo.getName() + "\n" +
                                "Identificador de Proceso (PID): " + pid + "\n" +
                                "Ruta de origen: " + rutaFichero,
                        "Proceso en Ejecución", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ioException) {
                JOptionPane.showMessageDialog(this,
                        "Error al intentar ejecutar el proceso:\n" + ioException.getMessage(),
                        "Fallo de Lanzamiento", JOptionPane.ERROR_MESSAGE);
            } catch (SecurityException secException) {
                JOptionPane.showMessageDialog(this,
                        "Permisos insuficientes del Sistema Operativo para ejecutar este archivo:\n" + secException.getMessage(),
                        "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        // Establecer Look and Feel nativo del sistema operativo (Windows)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            BuscadorProcesosApp app = new BuscadorProcesosApp();
            app.setVisible(true);
        });
    }
}
