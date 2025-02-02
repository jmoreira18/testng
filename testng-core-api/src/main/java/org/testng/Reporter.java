package org.testng;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.Utils;
import org.testng.util.Strings;

/**
 * This class is used for test methods to log messages that will be included in the HTML reports
 * generated by TestNG. <br>
 * <br>
 * <b>Implementation details</b> <br>
 * <br>
 * The reporter keeps a combined output of strings (in m_output) and also a record of which method
 * output which line. In order to do this, callers specify what the current method is with
 * setCurrentTestResult() and the Reporter maintains a mapping of each test result with a list of
 * integers. These integers are indices in the combined output (avoids duplicating the output).
 *
 * <p>Created on Nov 2, 2005
 *
 * @author cbeust
 */
public class Reporter {
  // When tests are run in parallel, each thread may be working with different
  // 'current test result'. Also, this value should be inherited if the test code
  // spawns its own thread.
  private static final ThreadLocal<ITestResult> m_currentTestResult =
      new InheritableThreadLocal<>();

  /** All output logged in a sequential order. */
  private static final List<String> m_output = new LinkedList<>();

  private static final Map<String, List<Integer>> m_methodOutputMap = Maps.newConcurrentMap();

  private static boolean m_escapeHtml = false;
  // This variable is responsible for persisting all output that is yet to be associated with any
  // valid TestResult objects.
  private static final ThreadLocal<List<String>> m_orphanedOutput = new InheritableThreadLocal<>();

  public static void setCurrentTestResult(ITestResult m) {
    m_currentTestResult.set(m);
  }

  public static List<String> getOutput() {
    return m_output;
  }

  /** Erase the content of all the output generated so far. */
  public static void clear() {
    m_methodOutputMap.clear();
    m_output.clear();
  }

  /** @return If true, use HTML entities for special HTML characters (&lt;, &gt;, &amp;, ...). */
  public static boolean getEscapeHtml() {
    return m_escapeHtml;
  }

  /**
   * @param escapeHtml If true, use HTML entities for special HTML characters (&lt;, &gt;, &amp;,
   *     ...).
   */
  public static void setEscapeHtml(boolean escapeHtml) {
    m_escapeHtml = escapeHtml;
  }

  private static synchronized void log(String s, ITestResult m) {
    // Escape for the HTML reports.
    if (m_escapeHtml) {
      s = Strings.escapeHtml(s);
    }

    if (m == null) {
      // Persist the output temporarily into a ThreadLocal String list.
      if (m_orphanedOutput.get() == null) {
        m_orphanedOutput.set(new LinkedList<>());
      }
      m_orphanedOutput.get().add(s);
      return;
    }

    // Synchronization needed to ensure the line number and m_output are updated atomically.
    int n = getOutput().size();

    List<Integer> lines = m_methodOutputMap.computeIfAbsent(m.id(), k -> Lists.newLinkedList());

    // Check if there was already some orphaned output for the current thread.
    if (m_orphanedOutput.get() != null) {
      n = n + m_orphanedOutput.get().size();
      getOutput().addAll(m_orphanedOutput.get());
      // Since we have already added all of the orphaned output to the current
      // TestResult, let's clear it off.
      m_orphanedOutput.remove();
    }
    lines.add(n);
    getOutput().add(s);
  }

  /**
   * Log the passed string to the HTML reports.
   *
   * @param s The message to log
   */
  public static void log(String s) {
    log(s, getCurrentTestResult());
  }

  /**
   * Log the passed string to the HTML reports if the current verbosity is equal to or greater than
   * the one passed as a parameter. If logToStandardOut is true, the string will also be printed on
   * standard out.
   *
   * @param s The message to log
   * @param level The verbosity of this message
   * @param logToStandardOut Whether to print this string on standard out too
   */
  public static void log(String s, int level, boolean logToStandardOut) {
    if (Utils.getVerbose() >= level) {
      log(s, getCurrentTestResult());
      if (logToStandardOut) {
        System.out.println(s);
      }
    }
  }

  /**
   * Log the passed string to the HTML reports. If logToStandardOut is true, the string will also be
   * printed on standard out.
   *
   * @param s The message to log
   * @param logToStandardOut Whether to print this string on standard out too
   */
  public static void log(String s, boolean logToStandardOut) {
    log(s, getCurrentTestResult());
    if (logToStandardOut) {
      System.out.println(s);
    }
  }
  /**
   * Log the passed string to the HTML reports if the current verbosity is equal to or greater than
   * the one passed as a parameter.
   *
   * @param s The message to log
   * @param level The verbosity of this message
   */
  public static void log(String s, int level) {
    if (Utils.getVerbose() >= level) {
      log(s, getCurrentTestResult());
    }
  }

  /** @return the current test result. */
  public static ITestResult getCurrentTestResult() {
    return m_currentTestResult.get();
  }

  public static synchronized List<String> getOutput(ITestResult tr) {
    List<String> result = Lists.newArrayList();
    if (tr == null) {
      // Guard against a possible NPE in scenarios wherein the test result object itself could be a
      // null value.
      return result;
    }
    List<Integer> lines = m_methodOutputMap.get(tr.id());
    if (lines != null) {
      for (Integer n : lines) {
        result.add(getOutput().get(n));
      }
    }

    return result;
  }
}
