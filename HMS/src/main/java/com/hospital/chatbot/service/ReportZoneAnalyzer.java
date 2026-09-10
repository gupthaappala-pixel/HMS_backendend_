package com.hospital.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReportZoneAnalyzer {

    public static class AnalysisResult {
        private String zoneStatus; // GREEN, YELLOW, RED
        private String aiSummary;
        private String healthMetricsJson;

        public AnalysisResult(String zoneStatus, String aiSummary, String healthMetricsJson) {
            this.zoneStatus = zoneStatus;
            this.aiSummary = aiSummary;
            this.healthMetricsJson = healthMetricsJson;
        }

        public String getZoneStatus() {
            return zoneStatus;
        }

        public String getAiSummary() {
            return aiSummary;
        }

        public String getHealthMetricsJson() {
            return healthMetricsJson;
        }
    }

    public AnalysisResult analyze(String testName, String resultValue, String referenceRange, String techRemarks) {
        if (resultValue == null || resultValue.trim().isEmpty()) {
            return new AnalysisResult(
                    "GREEN",
                    "Lab test record created. Awaiting quantitative results from laboratory processing.",
                    String.format("{\"testName\":\"%s\",\"zone\":\"GREEN\",\"observed\":0.0,\"minRef\":0.0,\"maxRef\":100.0}", escapeJson(testName))
            );
        }

        String lowerResult = resultValue.toLowerCase().trim();

        // Check text-based critical keywords
        if (lowerResult.contains("positive") || lowerResult.contains("reactive") || lowerResult.contains("critical") || lowerResult.contains("high risk")) {
            String summary = String.format("⚠️ AI Critical Alert: %s tested positive or critical (%s). Requires immediate physician review.", testName, resultValue);
            String json = buildJson(testName, 90.0, 0.0, 50.0, "RED", "Index");
            return new AnalysisResult("RED", summary, json);
        }

        if (lowerResult.contains("negative") || lowerResult.contains("non-reactive") || lowerResult.contains("normal")) {
            String summary = String.format("✅ AI Health Assessment: %s is within normal limits (%s). Patient is in the Healthy Green Zone.", testName, resultValue);
            String json = buildJson(testName, 20.0, 0.0, 50.0, "GREEN", "Status");
            return new AnalysisResult("GREEN", summary, json);
        }

        // Try extracting numeric values
        Double observedNum = extractFirstNumber(resultValue);
        Double minRef = null;
        Double maxRef = null;

        if (referenceRange != null && !referenceRange.trim().isEmpty()) {
            Double[] range = extractRangeNumbers(referenceRange);
            if (range != null) {
                minRef = range[0];
                maxRef = range[1];
            }
        }

        // Fallback default reference bounds if range parsing fails
        if (minRef == null || maxRef == null) {
            minRef = 10.0;
            maxRef = 100.0;
        }

        String zone = "GREEN";
        String summary = "";

        if (observedNum != null) {
            if (observedNum >= minRef && observedNum <= maxRef) {
                zone = "GREEN";
                summary = String.format("🟢 HEALTHY GREEN ZONE: %s result is %s (Reference Range: %s). Key parameters are optimal and stable.", testName, resultValue, referenceRange != null ? referenceRange : "Normal");
            } else {
                double diff = 0;
                if (observedNum > maxRef) {
                    diff = (observedNum - maxRef) / maxRef;
                } else if (observedNum < minRef && minRef > 0) {
                    diff = (minRef - observedNum) / minRef;
                }

                if (diff > 0.30) {
                    zone = "RED";
                    summary = String.format("🔴 CRITICAL RED ZONE: %s result is %s, which deviates significantly from normal bounds (%s). Immediate physician consult recommended.", testName, resultValue, referenceRange != null ? referenceRange : "Normal");
                } else {
                    zone = "YELLOW";
                    summary = String.format("🟡 WARNING YELLOW ZONE: %s result is %s, slightly outside reference range (%s). Requires routine clinical monitoring.", testName, resultValue, referenceRange != null ? referenceRange : "Normal");
                }
            }
        } else {
            summary = String.format("🟢 AI Analysis: %s report completed with result '%s'. Parameters evaluated for patient health record.", testName, resultValue);
        }

        if (techRemarks != null && !techRemarks.trim().isEmpty()) {
            summary += " Technician Notes: " + techRemarks;
        }

        String json = buildJson(testName, observedNum != null ? observedNum : 50.0, minRef, maxRef, zone, extractUnit(resultValue, referenceRange));
        return new AnalysisResult(zone, summary, json);
    }

    private Double extractFirstNumber(String input) {
        if (input == null) return null;
        Matcher m = Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(input);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Double[] extractRangeNumbers(String input) {
        if (input == null) return null;
        Matcher m = Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(input);
        Double first = null;
        Double second = null;
        if (m.find()) {
            try { first = Double.parseDouble(m.group()); } catch (Exception ignored) {}
        }
        if (m.find()) {
            try { second = Double.parseDouble(m.group()); } catch (Exception ignored) {}
        }
        if (first != null && second != null) {
            return new Double[]{ Math.min(first, second), Math.max(first, second) };
        } else if (first != null) {
            return new Double[]{ 0.0, first };
        }
        return null;
    }

    private String extractUnit(String val, String ref) {
        String combined = (val != null ? val : "") + " " + (ref != null ? ref : "");
        Matcher m = Pattern.compile("(?i)(mg/dl|g/dl|u/l|uiv/ml|mmol/l|pg/ml|%|bpm|mmhg)").matcher(combined);
        if (m.find()) {
            return m.group();
        }
        return "units";
    }

    private String buildJson(String testName, double observed, double minRef, double maxRef, String zone, String unit) {
        return String.format(
                "{\"testName\":\"%s\",\"observed\":%.2f,\"minRef\":%.2f,\"maxRef\":%.2f,\"zone\":\"%s\",\"unit\":\"%s\"}",
                escapeJson(testName), observed, minRef, maxRef, zone, escapeJson(unit)
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
