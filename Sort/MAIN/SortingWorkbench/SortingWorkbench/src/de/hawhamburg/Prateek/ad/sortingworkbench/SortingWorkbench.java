package de.hawhamburg.hamann.ad.sortingworkbench;

import de.hawhamburg.hamann.ad.sortingworkbench.gui.WorkbenchGUI;
import de.hawhamburg.hamann.ad.sortingworkbench.sorter.*;
import de.hawhamburg.hamann.ad.sortingworkbench.util.ListValidator;
import de.hawhamburg.hamann.ad.sortingworkbench.util.ProgressEvent;
import de.hawhamburg.hamann.ad.sortingworkbench.util.WorkbenchProgressListener;

import javax.swing.*;
import java.util.*;

/**
 * Das Herzstück des Projekts. Hier werden Sortieralgorithmen
 * konfigurierbar ausgeführt (<code>executeWorkbench</code>).
 * <p>
 * Die Workbench wir mit einer Liste von <code>Sorter</code>n initialisiert.
 * Jeder Sorter steht für einen Sortieralgorithmus.
 * </p>
 * Die Ausführung kann über die <code>WorkbenchConfig</code> dahingehend konfiguriert werden,
 * dass eine bestimme Anzahl an Läufen durchgeführt werden, die bei einer Grundzahl an zu
 * sortierenden Elementen starten und pro Schritt um eine definierte Anzahl an
 * Elementen erhöht werden.
 * <p>
 * Bei jedem Schritt werden alle übergebenen Sortieralgorithmen für drei Listen ausgeführt:
 * 1. eine Liste mit Pseudozufallszahlen
 * 2. eine aufsteigend sortierte Liste
 * 3. eine absteigend sortierte Liste
 * 4. eine
 * </p>
 * Die Ergebnisse eines Laufs können über einen Listener während der Ausführung ausgewertet werden.
 */
public class SortingWorkbench {

    /**
     * Liste der auszuführenden Sortieralgorithmen
     */
    private final List<Sorter> sorters;

    /**
     * Konfiguration der Workbench (Anzahl Läufe, Startanzahl an Elementen, ...)
     */
    private final WorkbenchConfig config;

    /**
     * Liste der Fortschritts-Listener
     */
    private final List<WorkbenchProgressListener> progressListener = new ArrayList<>();

    /**
     * Erzeugt eine neuen <code>SortingWorkbench</code> mit den auszuführenden
     * Sortieralgorithmen <code>sorters</code> und den Einstellungen in <code>config</code>.
     * @param sorters Die auszuführenden <Code>Sorter</Code>
     * @param config Einstellungen zu Ausführung
     */
    public SortingWorkbench(List<Sorter> sorters, WorkbenchConfig config) {
        this.sorters = sorters;
        this.config = config;
    }

    public void addProgressListener(WorkbenchProgressListener l) {
        this.progressListener.add(l);
    }

    public void removeProgressListener(WorkbenchProgressListener l) {
        this.progressListener.remove(l);
    }

    private void fireWorkbenchProgress(ProgressEvent event) {
        for (WorkbenchProgressListener l : this.progressListener) {
            l.reportProgress(event);
        }
    }

    public WorkbenchConfig getConfig() {
        return config;
    }

    public void executeWorkbench() {

        List<Integer> randomElements;
        List<Integer> orderedElements;
        List<Integer> reverseOrderedElements;
        List<Integer> partialOrderedElements;
        List<Integer> toSort;

        DataRetriever dr = new DataRetriever();

        while (!this.getConfig().isFinished()) {
            RunInfo runConfig = this.config.getNextRunConfig();

            randomElements = dr.createRandomIntegerList(runConfig.getNumElements());
            orderedElements = dr.createOrderedIntegerList(runConfig.getNumElements());
            reverseOrderedElements = dr.createReverseOrderedIntegerList(runConfig.getNumElements());
            partialOrderedElements = dr.createPartiallyOrderedIntegerList(runConfig.getNumElements(), runConfig.getPartialOrderedListSize());

            for (Sorter sorter : sorters) {
                toSort = new ArrayList<>(randomElements);
                sorter.sort(toSort, runConfig.getRandomMetricsFor(sorter));
                assert ListValidator.validateOrder(toSort) : "Randomized list was not sorted!";

                toSort = new ArrayList<>(orderedElements);
                sorter.sort(toSort, runConfig.getOrderedMetricsFor(sorter));
                assert ListValidator.validateOrder(toSort) : "Ordered list was not sorted!";

                toSort = new ArrayList<>(reverseOrderedElements);
                sorter.sort(toSort, runConfig.getReverseOrderedMetricsFor(sorter));
                assert ListValidator.validateOrder(toSort) : "Reverse ordered list was not sorted!";

                toSort = new ArrayList<>(partialOrderedElements);
                sorter.sort(toSort, runConfig.getPartialOrderedMetricsFor(sorter));
                assert ListValidator.validateOrder(toSort) : "Partial ordered list was not sorted!";
            }

            this.fireWorkbenchProgress(new ProgressEvent(runConfig, "Run %05d with %d elements finished!".formatted(this.getConfig().getCurrentRun(), runConfig.getNumElements())));
        }
    }

    private static final Sorter[] knownSorters = new Sorter[] {
        new SelectionSort(),
        new InsertionSort(),
        new BubbleSort(),
        new QuickSort(),
        new MergeSort(),
            new HybridQuickInsertionSorter()
    };

    /**
     * Beispielausführung einer WorkBench.
     * Kann an die eigenen Bedürfnisse angepasst werden.
     *
     * @param args Startargumente. Aktuell möglich "-v" zum Visualisieren der Sortieralgorithmen.
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("-v")) {
            visualizeSortingAlgorithm();
        } else {
            executeCompleteWorkbench();
        }
    }

    private static void visualizeSortingAlgorithm() {
        final int numElements = 500;

        JFrame frame = new WorkbenchGUI(knownSorters, numElements);
        frame.setVisible(true);
    }

    private static void executeCompleteWorkbench() {
        List<Sorter> sorter = Arrays.asList(knownSorters);

        WorkbenchConfig config = new WorkbenchConfig(sorter,20, 1000, 1000, 50);

        SortingWorkbench wb = new SortingWorkbench(sorter, config);

        wb.addProgressListener(event -> {
            System.out.println(event.getMessage());

            for (Sorter s : sorter) {
                SortingMetrics metRandom = event.getRunConfig().getRandomMetricsFor(s);
                SortingMetrics metOrdered = event.getRunConfig().getOrderedMetricsFor(s);
                SortingMetrics metReverse = event.getRunConfig().getReverseOrderedMetricsFor(s);
                SortingMetrics metPartial = event.getRunConfig().getPartialOrderedMetricsFor(s);

                System.out.printf("  Results for %s:\n", s.getName());
                System.out.printf("    Swaps random: %d, ordered: %d, reverse: %d, partial ord.: %d\n",
                        metRandom.getNumMoves(),
                        metOrdered.getNumMoves(),
                        metReverse.getNumMoves(),
                        metPartial.getNumMoves());
                System.out.printf("    Compares random: %d, ordered: %d, reverse: %d, partial ord.: %d\n",
                        metRandom.getNumCompares(),
                        metOrdered.getNumCompares(),
                        metReverse.getNumCompares(),
                        metPartial.getNumCompares());
            }
        });

        wb.executeWorkbench();
    }
}
