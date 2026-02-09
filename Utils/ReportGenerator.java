package Utils;

import Data.ElectionData;
import javax.swing.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Lightweight report generator fallback when external PDF/Excel libs are unavailable.
 * Produces a plain text report summarizing vote counts.
 */
public class ReportGenerator {

    public static boolean generateTextReport(String filePath) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
            out.println("OFFICIAL ELECTION RESULTS REPORT");
            out.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            out.println();

            Map<String, Integer> voteCounts = ElectionData.getVoteCounts();
            int totalVotes = ElectionData.getTotalVotesCast();
            int totalVoters = ElectionData.getTotalRegisteredVoters();

            out.println("Total Registered Voters: " + totalVoters);
            out.println("Total Votes Cast: " + totalVotes);
            out.println();

            out.printf("%-30s %-10s %-10s\n", "Candidate", "Votes", "Percent");
            out.println("---------------------------------------------------------");
            for (Map.Entry<String, Integer> e : voteCounts.entrySet()) {
                int v = e.getValue();
                double pct = totalVotes > 0 ? (v * 100.0 / totalVotes) : 0.0;
                out.printf("%-30s %-10d %-9.2f%%\n", e.getKey(), v, pct);
            }

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void showReportDialog(JFrame parent) {
        String[] options = {"Text Report", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent,
            "Select report format:",
            "Generate Report",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice == 0) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = "election_report_" + timestamp + ".txt";
            if (generateTextReport(path)) {
                CommonUtils.showSuccessDialog(parent, "Report generated: " + path);
            } else {
                CommonUtils.showErrorDialog(parent, "Failed to generate report.");
            }
        }
    }
}