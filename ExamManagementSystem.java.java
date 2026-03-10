import java.util.*;
import java.text.SimpleDateFormat;

// ============================================================
//   SMART ONLINE EXAMINATION MANAGEMENT SYSTEM
//   Single File Java Program - All Classes Included
//   Run: javac ExamManagementSystem.java && java ExamManagementSystem
// ============================================================

// ─────────────────────────────────────────────────────────────
// CLASS 1: User — stores student/admin account details
// ─────────────────────────────────────────────────────────────
class User {
    private String name;
    private String email;
    private String password;
    private String rollNumber;
    private String department;
    private String semester;
    private String phone;
    private String dob;
    private String role; // "student" or "admin"

    public User(String name, String email, String password,
                String rollNumber, String department,
                String semester, String phone, String dob, String role) {
        this.name       = name;
        this.email      = email;
        this.password   = password;
        this.rollNumber = rollNumber;
        this.department = department;
        this.semester   = semester;
        this.phone      = phone;
        this.dob        = dob;
        this.role       = role;
    }

    // ── Getters ──
    public String getName()       { return name; }
    public String getEmail()      { return email; }
    public String getPassword()   { return password; }
    public String getRollNumber() { return rollNumber; }
    public String getDepartment() { return department; }
    public String getSemester()   { return semester; }
    public String getPhone()      { return phone; }
    public String getDob()        { return dob; }
    public String getRole()       { return role; }

    // ── Setters ──
    public void setName(String name)             { this.name = name; }
    public void setPassword(String password)     { this.password = password; }
    public void setDepartment(String department) { this.department = department; }
    public void setSemester(String semester)     { this.semester = semester; }
    public void setPhone(String phone)           { this.phone = phone; }
    public void setDob(String dob)               { this.dob = dob; }

    public boolean authenticate(String email, String password) {
        return this.email.equalsIgnoreCase(email) && this.password.equals(password);
    }

    public void displayProfile() {
        UI.printBox("MY PROFILE");
        System.out.printf("  %-18s: %s%n", "Name",        name);
        System.out.printf("  %-18s: %s%n", "Email",       email);
        System.out.printf("  %-18s: %s%n", "Roll Number", rollNumber);
        System.out.printf("  %-18s: %s%n", "Department",  department);
        System.out.printf("  %-18s: %s%n", "Semester",    semester);
        System.out.printf("  %-18s: %s%n", "Phone",       phone);
        System.out.printf("  %-18s: %s%n", "Date of Birth", dob);
        System.out.printf("  %-18s: %s%n", "Role",        role.toUpperCase());
        UI.printLine();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 2: Question — represents a single MCQ
// ─────────────────────────────────────────────────────────────
class Question {
    private int    id;
    private String subject;
    private String questionText;
    private String[] options;       // exactly 4 options
    private int    correctIndex;    // 0-based index of correct option

    public Question(int id, String subject, String questionText,
                    String[] options, int correctIndex) {
        this.id            = id;
        this.subject       = subject;
        this.questionText  = questionText;
        this.options       = options;
        this.correctIndex  = correctIndex;
    }

    public int    getId()           { return id; }
    public String getSubject()      { return subject; }
    public String getQuestionText() { return questionText; }
    public String[] getOptions()    { return options; }
    public int    getCorrectIndex() { return correctIndex; }

    public boolean isCorrect(int chosen) {
        return chosen == correctIndex;
    }

    public void display(int currentNum, int total) {
        UI.printLine();
        System.out.println("  Question " + currentNum + " of " + total
                + "  |  Subject: " + subject);
        UI.printLine();
        System.out.println("  " + questionText);
        System.out.println();
        char label = 'A';
        for (String opt : options) {
            System.out.println("    [" + label + "] " + opt);
            label++;
        }
        System.out.println();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 3: QuestionBank — holds all exam questions
// ─────────────────────────────────────────────────────────────
class QuestionBank {
    private List<Question> questions = new ArrayList<>();

    public QuestionBank() {
        loadQuestions();
    }

    private void loadQuestions() {
        questions.add(new Question(1, "Data Structures",
            "Which data structure follows the Last In First Out (LIFO) principle?",
            new String[]{"Queue", "Stack", "Linked List", "Tree"}, 1));

        questions.add(new Question(2, "Operating Systems",
            "What does CPU stand for?",
            new String[]{"Central Processing Unit", "Core Processing Unit",
                         "Central Program Utility", "Computer Processing Unit"}, 0));

        questions.add(new Question(3, "Networking",
            "Which protocol is used to send emails?",
            new String[]{"FTP", "HTTP", "SMTP", "TCP"}, 2));

        questions.add(new Question(4, "DBMS",
            "SQL stands for:",
            new String[]{"Structured Query Language", "Simple Query Language",
                         "Sequential Query Logic", "Structured Question List"}, 0));

        questions.add(new Question(5, "Programming",
            "Which of the following is NOT an Object-Oriented Programming concept?",
            new String[]{"Encapsulation", "Polymorphism", "Compilation", "Inheritance"}, 2));

        questions.add(new Question(6, "Data Structures",
            "The time complexity of Binary Search is:",
            new String[]{"O(n)", "O(log n)", "O(n^2)", "O(1)"}, 1));

        questions.add(new Question(7, "DBMS",
            "Which normal form eliminates transitive dependencies?",
            new String[]{"1NF", "2NF", "3NF", "BCNF"}, 2));

        questions.add(new Question(8, "Networking",
            "What is the maximum number of usable host IPs in a Class C network?",
            new String[]{"256", "254", "512", "128"}, 1));

        questions.add(new Question(9, "Operating Systems",
            "Which scheduling algorithm gives minimum average waiting time?",
            new String[]{"FCFS", "Round Robin", "SJF (Shortest Job First)", "Priority"}, 2));

        questions.add(new Question(10, "Programming",
            "Which keyword prevents inheritance in Java?",
            new String[]{"static", "final", "private", "abstract"}, 1));

        questions.add(new Question(11, "Data Structures",
            "A complete binary tree with n nodes has height of order:",
            new String[]{"O(n)", "O(log n)", "O(n log n)", "O(1)"}, 1));

        questions.add(new Question(12, "Networking",
            "Which OSI layer is responsible for routing packets?",
            new String[]{"Data Link Layer", "Transport Layer",
                         "Network Layer", "Session Layer"}, 2));

        questions.add(new Question(13, "DBMS",
            "Which SQL command is used to permanently remove a table?",
            new String[]{"DELETE", "REMOVE", "DROP", "TRUNCATE"}, 2));

        questions.add(new Question(14, "Operating Systems",
            "Which condition must be broken to prevent deadlock?",
            new String[]{"Mutual Exclusion", "Hold and Wait",
                         "No Preemption", "Any of the above"}, 3));

        questions.add(new Question(15, "Programming",
            "Which sorting algorithm has the best average-case time complexity?",
            new String[]{"Bubble Sort", "Insertion Sort", "Merge Sort", "Selection Sort"}, 2));
    }

    public List<Question> getAllQuestions() { return questions; }
    public int getTotalQuestions()          { return questions.size(); }
}

// ─────────────────────────────────────────────────────────────
// CLASS 4: ExamResult — stores the result after submission
// ─────────────────────────────────────────────────────────────
class ExamResult {
    private User     student;
    private int[]    answers;       // -1 = skipped
    private int      score;
    private int      totalQuestions;
    private long     timeTakenSeconds;
    private String   submittedAt;
    private List<Question> questions;

    public ExamResult(User student, int[] answers, List<Question> questions, long timeTakenSeconds) {
        this.student          = student;
        this.answers          = answers;
        this.questions        = questions;
        this.totalQuestions   = questions.size();
        this.timeTakenSeconds = timeTakenSeconds;
        this.submittedAt      = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        calculateScore();
    }

    private void calculateScore() {
        score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (answers[i] != -1 && questions.get(i).isCorrect(answers[i])) {
                score++;
            }
        }
    }

    public int    getScore()          { return score; }
    public int    getTotalQuestions() { return totalQuestions; }
    public double getPercentage()     { return (score * 100.0) / totalQuestions; }
    public boolean isPassed()         { return getPercentage() >= 60.0; }

    public int getCorrectCount() {
        int c = 0;
        for (int i = 0; i < questions.size(); i++)
            if (answers[i] != -1 && questions.get(i).isCorrect(answers[i])) c++;
        return c;
    }

    public int getWrongCount() {
        int w = 0;
        for (int i = 0; i < questions.size(); i++)
            if (answers[i] != -1 && !questions.get(i).isCorrect(answers[i])) w++;
        return w;
    }

    public int getSkippedCount() {
        int s = 0;
        for (int ans : answers) if (ans == -1) s++;
        return s;
    }

    public void displayResult() {
        UI.printBox("EXAMINATION RESULT");
        System.out.printf("  %-22s: %s%n",  "Student Name",    student.getName());
        System.out.printf("  %-22s: %s%n",  "Roll Number",     student.getRollNumber());
        System.out.printf("  %-22s: %s%n",  "Department",      student.getDepartment());
        System.out.printf("  %-22s: %s%n",  "Submitted At",    submittedAt);
        System.out.printf("  %-22s: %d min %d sec%n", "Time Taken",
                timeTakenSeconds / 60, timeTakenSeconds % 60);
        UI.printLine();
        System.out.printf("  %-22s: %d / %d%n",   "Score",       score, totalQuestions);
        System.out.printf("  %-22s: %.1f%%%n",     "Percentage",  getPercentage());
        System.out.printf("  %-22s: %d%n",         "Correct",     getCorrectCount());
        System.out.printf("  %-22s: %d%n",         "Wrong",       getWrongCount());
        System.out.printf("  %-22s: %d%n",         "Skipped",     getSkippedCount());
        UI.printLine();

        String status = isPassed() ? "*** PASSED ***" : "*** FAILED ***";
        String grade  = getGrade();
        System.out.printf("  %-22s: %s%n",  "Status",  status);
        System.out.printf("  %-22s: %s%n",  "Grade",   grade);
        System.out.printf("  %-22s: %s%n",  "Remarks", getRemarks());
        UI.printLine();
    }

    public void displayDetailedReview() {
        UI.printBox("DETAILED ANSWER REVIEW");
        for (int i = 0; i < questions.size(); i++) {
            Question q   = questions.get(i);
            int      ans = answers[i];
            String status;
            if (ans == -1)                 status = "[SKIPPED]";
            else if (q.isCorrect(ans))     status = "[CORRECT]";
            else                           status = "[WRONG]  ";

            System.out.println("  Q" + (i + 1) + " " + status + " " + q.getQuestionText());

            if (ans != -1 && !q.isCorrect(ans)) {
                System.out.println("       Your Answer   : " + (char)('A' + ans)
                        + ". " + q.getOptions()[ans]);
            }
            if (ans == -1 || !q.isCorrect(ans)) {
                System.out.println("       Correct Answer: " + (char)('A' + q.getCorrectIndex())
                        + ". " + q.getOptions()[q.getCorrectIndex()]);
            }
            System.out.println();
        }
        UI.printLine();
    }

    public void displaySubjectWise() {
        UI.printBox("SUBJECT-WISE PERFORMANCE");
        Map<String, int[]> subjectMap = new LinkedHashMap<>();
        // int[0] = correct, int[1] = total
        for (int i = 0; i < questions.size(); i++) {
            String subject = questions.get(i).getSubject();
            subjectMap.putIfAbsent(subject, new int[]{0, 0});
            subjectMap.get(subject)[1]++;
            if (answers[i] != -1 && questions.get(i).isCorrect(answers[i]))
                subjectMap.get(subject)[0]++;
        }
        for (Map.Entry<String, int[]> entry : subjectMap.entrySet()) {
            String sub = entry.getKey();
            int    c   = entry.getValue()[0];
            int    t   = entry.getValue()[1];
            double pct = (c * 100.0) / t;
            String bar = buildBar(pct);
            System.out.printf("  %-22s: %d/%d  %s  %.0f%%%n", sub, c, t, bar, pct);
        }
        UI.printLine();
    }

    private String buildBar(double pct) {
        int filled = (int)(pct / 10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "█" : "░");
        sb.append("]");
        return sb.toString();
    }

    private String getGrade() {
        double p = getPercentage();
        if (p >= 90) return "A+ (Outstanding)";
        if (p >= 80) return "A  (Excellent)";
        if (p >= 70) return "B  (Good)";
        if (p >= 60) return "C  (Average / Pass)";
        if (p >= 50) return "D  (Below Average)";
        return "F  (Fail)";
    }

    private String getRemarks() {
        double p = getPercentage();
        if (p >= 90) return "Exceptional performance! Keep it up.";
        if (p >= 80) return "Great work! You are doing very well.";
        if (p >= 70) return "Good performance. Aim higher next time.";
        if (p >= 60) return "You passed. Focus on weak areas.";
        return "You did not pass. Please revise and retry.";
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 5: Timer — countdown timer running in a separate thread
// ─────────────────────────────────────────────────────────────
class ExamTimer extends Thread {
    private int     totalSeconds;
    private volatile boolean running  = true;
    private volatile boolean expired  = false;
    private volatile boolean paused   = false;

    public ExamTimer(int minutes) {
        this.totalSeconds = minutes * 60;
        setDaemon(true); // thread dies when main thread exits
    }

    @Override
    public void run() {
        while (totalSeconds > 0 && running) {
            if (!paused) {
                totalSeconds--;
                if (totalSeconds == 300)
                    UI.warn("  *** WARNING: Only 5 minutes remaining! ***");
                if (totalSeconds == 60)
                    UI.warn("  *** CRITICAL: Only 1 minute remaining! ***");
                if (totalSeconds == 0) { expired = true; break; }
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }

    public void stopTimer()  { running = false; }
    public boolean isExpired() { return expired; }

    public String getFormattedTime() {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public int getRemainingSeconds() { return totalSeconds; }
}

// ─────────────────────────────────────────────────────────────
// CLASS 6: Session — manages the logged-in user session
// ─────────────────────────────────────────────────────────────
class Session {
    private User    currentUser;
    private boolean loggedIn    = false;
    private Date    loginTime;

    public void login(User user) {
        this.currentUser = user;
        this.loggedIn    = true;
        this.loginTime   = new Date();
    }

    public void logout() {
        this.currentUser = null;
        this.loggedIn    = false;
        this.loginTime   = null;
    }

    public boolean isLoggedIn()    { return loggedIn; }
    public User    getCurrentUser(){ return currentUser; }

    public String getSessionInfo() {
        if (!loggedIn) return "No active session.";
        String started = new SimpleDateFormat("HH:mm:ss").format(loginTime);
        return "Logged in as: " + currentUser.getName()
             + " | Role: " + currentUser.getRole().toUpperCase()
             + " | Session started: " + started;
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 7: UserDatabase — in-memory user store
// ─────────────────────────────────────────────────────────────
class UserDatabase {
    private Map<String, User> users = new HashMap<>();

    public UserDatabase() {
        // Seed default users
        addUser(new User("Dr. Aisha Patel",  "admin@exam.edu",   "Admin@123",
                         "ADMIN001", "Computer Science", "Faculty",
                         "9876543210", "1985-04-12", "admin"));

        addUser(new User("Arjun Mehta",      "student@exam.edu", "Student@123",
                         "CS2021047",  "Computer Science", "6th Semester",
                         "8765432109", "2002-08-25", "student"));

        addUser(new User("Priya Sharma",     "priya@exam.edu",   "Priya@2024",
                         "CS2021088",  "Information Technology", "6th Semester",
                         "7654321098", "2002-11-03", "student"));
    }

    public void addUser(User u)   { users.put(u.getEmail().toLowerCase(), u); }

    public User findByEmail(String email) {
        return users.get(email.toLowerCase());
    }

    public User authenticate(String email, String password) {
        User u = findByEmail(email);
        if (u != null && u.getPassword().equals(password)) return u;
        return null;
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 8: UI — static helpers for console formatting
// ─────────────────────────────────────────────────────────────
class UI {
    static final String LINE  = "  " + "=".repeat(60);
    static final String DLINE = "  " + "-".repeat(60);

    static void printLine()  { System.out.println(LINE); }
    static void printDLine() { System.out.println(DLINE); }
    static void blank()      { System.out.println(); }

    static void printBox(String title) {
        blank();
        printLine();
        int pad = (60 - title.length()) / 2;
        System.out.println("  " + " ".repeat(Math.max(0, pad)) + title);
        printLine();
    }

    static void warn(String msg) {
        System.out.println();
        System.out.println("  [!] " + msg);
        System.out.println();
    }

    static void success(String msg) { System.out.println("  [OK] " + msg); }
    static void error(String msg)   { System.out.println("  [ERR] " + msg); }
    static void info(String msg)    { System.out.println("  [i]  " + msg); }

    static void header() {
        blank();
        printLine();
        System.out.println("        SMART ONLINE EXAMINATION MANAGEMENT SYSTEM");
        System.out.println("                  Educational Institute Portal");
        printLine();
        blank();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 9: ProfileManager — handles profile & password changes
// ─────────────────────────────────────────────────────────────
class ProfileManager {
    private Scanner scanner;

    public ProfileManager(Scanner scanner) {
        this.scanner = scanner;
    }

    public void updateProfile(User user) {
        UI.printBox("UPDATE PROFILE");
        System.out.println("  Press ENTER to keep current value.\n");

        System.out.print("  Name        [" + user.getName() + "]: ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) user.setName(name);

        System.out.print("  Department  [" + user.getDepartment() + "]: ");
        String dept = scanner.nextLine().trim();
        if (!dept.isEmpty()) user.setDepartment(dept);

        System.out.print("  Semester    [" + user.getSemester() + "]: ");
        String sem = scanner.nextLine().trim();
        if (!sem.isEmpty()) user.setSemester(sem);

        System.out.print("  Phone       [" + user.getPhone() + "]: ");
        String phone = scanner.nextLine().trim();
        if (!phone.isEmpty()) {
            if (phone.matches("\\d{10}")) user.setPhone(phone);
            else UI.error("Invalid phone number — keeping old value.");
        }

        System.out.print("  Date of Birth [" + user.getDob() + "] (YYYY-MM-DD): ");
        String dob = scanner.nextLine().trim();
        if (!dob.isEmpty()) {
            if (dob.matches("\\d{4}-\\d{2}-\\d{2}")) user.setDob(dob);
            else UI.error("Invalid date format — keeping old value.");
        }

        UI.success("Profile updated successfully!");
        UI.printLine();
    }

    public void changePassword(User user) {
        UI.printBox("CHANGE PASSWORD");

        System.out.print("  Enter current password : ");
        String current = scanner.nextLine().trim();
        if (!current.equals(user.getPassword())) {
            UI.error("Incorrect current password. Operation cancelled.");
            return;
        }

        System.out.print("  Enter new password     : ");
        String newPass = scanner.nextLine().trim();

        if (newPass.length() < 8) {
            UI.error("Password must be at least 8 characters."); return;
        }
        if (!newPass.matches(".*[A-Z].*")) {
            UI.error("Password must contain at least one uppercase letter."); return;
        }
        if (!newPass.matches(".*[0-9].*")) {
            UI.error("Password must contain at least one digit."); return;
        }

        System.out.print("  Confirm new password   : ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equals(newPass)) {
            UI.error("Passwords do not match. Operation cancelled."); return;
        }

        user.setPassword(newPass);
        UI.success("Password changed successfully!");
        UI.printLine();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 10: ExamController — drives the exam flow
// ─────────────────────────────────────────────────────────────
class ExamController {
    private Scanner      scanner;
    private QuestionBank bank;
    private static final int EXAM_MINUTES = 30;

    public ExamController(Scanner scanner) {
        this.scanner = scanner;
        this.bank    = new QuestionBank();
    }

    public ExamResult startExam(User student) {
        showInstructions();
        System.out.print("  Type START to begin the exam (or BACK to cancel): ");
        String input = scanner.nextLine().trim().toUpperCase();
        if (!input.equals("START")) {
            UI.info("Exam cancelled. Returning to menu.");
            return null;
        }

        List<Question> questions = bank.getAllQuestions();
        int[] answers = new int[questions.size()];
        Arrays.fill(answers, -1); // -1 = unanswered

        ExamTimer timer = new ExamTimer(EXAM_MINUTES);
        timer.start();

        long startTime = System.currentTimeMillis();

        UI.printBox("EXAMINATION IN PROGRESS");
        UI.info("Type A / B / C / D to answer. Type SKIP to skip. Type STATUS to see timer. Type SUBMIT to finish early.");
        UI.printLine();

        int i = 0;
        while (i < questions.size()) {

            // Auto-submit if time expired
            if (timer.isExpired()) {
                UI.warn("TIME'S UP! Exam is being auto-submitted.");
                break;
            }

            Question q = questions.get(i);
            System.out.println();
            System.out.println("  [Time Remaining: " + timer.getFormattedTime() + "]");
            q.display(i + 1, questions.size());

            // Show current answer if already set
            if (answers[i] != -1) {
                System.out.println("  (Current answer: " + (char)('A' + answers[i]) + ")");
            }

            System.out.print("  Your answer (A/B/C/D | SKIP | PREV | SUBMIT | STATUS): ");
            String resp = scanner.nextLine().trim().toUpperCase();

            if (timer.isExpired()) {
                UI.warn("TIME'S UP! Auto-submitting.");
                break;
            }

            switch (resp) {
                case "A": answers[i] = 0; i++; break;
                case "B": answers[i] = 1; i++; break;
                case "C": answers[i] = 2; i++; break;
                case "D": answers[i] = 3; i++; break;
                case "SKIP":
                    answers[i] = -1; i++;
                    UI.info("Question skipped.");
                    break;
                case "PREV":
                    if (i > 0) { i--; UI.info("Going back to previous question."); }
                    else UI.error("You are already on the first question.");
                    break;
                case "STATUS":
                    showExamStatus(answers, i + 1, questions.size(), timer);
                    break;
                case "SUBMIT":
                    System.out.print("  Are you sure you want to submit? (YES/NO): ");
                    String confirm = scanner.nextLine().trim().toUpperCase();
                    if (confirm.equals("YES")) { i = questions.size(); } // exit loop
                    break;
                default:
                    UI.error("Invalid input. Enter A, B, C, D, SKIP, PREV, SUBMIT, or STATUS.");
            }
        }

        timer.stopTimer();
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        ExamResult result = new ExamResult(student, answers, questions, timeTaken);
        return result;
    }

    private void showInstructions() {
        UI.printBox("EXAMINATION INSTRUCTIONS");
        System.out.println("  Course   : Computer Science MCQ Examination");
        System.out.println("  Questions: " + bank.getTotalQuestions());
        System.out.println("  Duration : " + EXAM_MINUTES + " minutes");
        System.out.println("  Marking  : +1 for correct, 0 for wrong/skipped");
        System.out.println("  Pass Mark: 60% (" + (int)(bank.getTotalQuestions() * 0.6) + "/" + bank.getTotalQuestions() + ")");
        UI.printLine();
        System.out.println("  CONTROLS DURING EXAM:");
        System.out.println("    A / B / C / D  — Select answer and go to next question");
        System.out.println("    SKIP           — Skip current question");
        System.out.println("    PREV           — Go back to previous question");
        System.out.println("    STATUS         — View time remaining and progress");
        System.out.println("    SUBMIT         — Submit exam early");
        UI.printLine();
        System.out.println("  RULES:");
        System.out.println("    1. The exam auto-submits when the timer reaches zero.");
        System.out.println("    2. You can revisit previous questions using PREV.");
        System.out.println("    3. There is no negative marking.");
        System.out.println("    4. Do not close the terminal during the exam.");
        UI.printLine();
    }

    private void showExamStatus(int[] answers, int current, int total, ExamTimer timer) {
        int answered = 0, skipped = 0;
        for (int a : answers) {
            if (a != -1) answered++;
            else skipped++;
        }
        UI.printLine();
        System.out.println("  EXAM STATUS");
        System.out.printf("  %-22s: %s%n",   "Time Remaining",    timer.getFormattedTime());
        System.out.printf("  %-22s: %d / %d%n", "Current Question", current, total);
        System.out.printf("  %-22s: %d%n",   "Answered",          answered);
        System.out.printf("  %-22s: %d%n",   "Unanswered/Skipped", skipped);
        UI.printLine();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 11: LoginManager — handles authentication flow
// ─────────────────────────────────────────────────────────────
class LoginManager {
    private Scanner      scanner;
    private UserDatabase db;
    private Session      session;
    private static final int MAX_ATTEMPTS = 3;

    public LoginManager(Scanner scanner, UserDatabase db, Session session) {
        this.scanner = scanner;
        this.db      = db;
        this.session = session;
    }

    public boolean login() {
        UI.printBox("SECURE LOGIN");
        UI.info("Demo accounts:");
        System.out.println("    student@exam.edu / Student@123");
        System.out.println("    admin@exam.edu   / Admin@123");
        UI.printLine();

        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            System.out.print("  Email    : ");
            String email = scanner.nextLine().trim();

            System.out.print("  Password : ");
            String password = scanner.nextLine().trim();

            User user = db.authenticate(email, password);
            if (user != null) {
                session.login(user);
                UI.success("Login successful! Welcome, " + user.getName() + ".");
                UI.printLine();
                return true;
            }

            attempts++;
            int remaining = MAX_ATTEMPTS - attempts;
            if (remaining > 0) {
                UI.error("Invalid credentials. " + remaining + " attempt(s) remaining.");
            } else {
                UI.error("Maximum attempts reached. Access denied.");
            }
        }
        return false;
    }

    public void logout(Session session) {
        String name = session.getCurrentUser().getName();
        session.logout();
        UI.success("You have been logged out securely. Goodbye, " + name + "!");
        UI.printLine();
    }
}

// ─────────────────────────────────────────────────────────────
// CLASS 12: Menu — draws and handles all menus
// ─────────────────────────────────────────────────────────────
class Menu {
    private Scanner        scanner;
    private Session        session;
    private UserDatabase   db;
    private LoginManager   loginMgr;
    private ProfileManager profileMgr;
    private ExamController examCtrl;
    private ExamResult     lastResult;

    public Menu(Scanner scanner) {
        this.scanner    = scanner;
        this.db         = new UserDatabase();
        this.session    = new Session();
        this.loginMgr   = new LoginManager(scanner, db, session);
        this.profileMgr = new ProfileManager(scanner);
        this.examCtrl   = new ExamController(scanner);
    }

    public void run() {
        UI.header();

        if (!loginMgr.login()) {
            System.out.println("\n  System locked. Exiting.\n");
            return;
        }

        boolean running = true;
        while (running) {
            String role = session.getCurrentUser().getRole();
            if (role.equals("admin")) running = showAdminMenu();
            else                      running = showStudentMenu();
        }
    }

    // ── STUDENT MENU ──────────────────────────────────────────
    private boolean showStudentMenu() {
        UI.printBox("STUDENT DASHBOARD");
        System.out.println("  " + session.getSessionInfo());
        UI.printLine();
        System.out.println("    [1] View My Profile");
        System.out.println("    [2] Update Profile");
        System.out.println("    [3] Change Password");
        System.out.println("    [4] Start Examination");
        System.out.println("    [5] View Last Result");
        System.out.println("    [6] View Detailed Answer Review");
        System.out.println("    [7] View Subject-Wise Performance");
        System.out.println("    [0] Logout");
        UI.printLine();
        System.out.print("  Enter choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                session.getCurrentUser().displayProfile();
                pause();
                break;
            case "2":
                profileMgr.updateProfile(session.getCurrentUser());
                pause();
                break;
            case "3":
                profileMgr.changePassword(session.getCurrentUser());
                pause();
                break;
            case "4":
                ExamResult result = examCtrl.startExam(session.getCurrentUser());
                if (result != null) {
                    lastResult = result;
                    result.displayResult();
                }
                pause();
                break;
            case "5":
                if (lastResult != null) lastResult.displayResult();
                else UI.info("No exam attempted yet in this session.");
                pause();
                break;
            case "6":
                if (lastResult != null) lastResult.displayDetailedReview();
                else UI.info("No exam attempted yet in this session.");
                pause();
                break;
            case "7":
                if (lastResult != null) lastResult.displaySubjectWise();
                else UI.info("No exam attempted yet in this session.");
                pause();
                break;
            case "0":
                loginMgr.logout(session);
                return false;
            default:
                UI.error("Invalid choice. Please try again.");
        }
        return true;
    }

    // ── ADMIN MENU ────────────────────────────────────────────
    private boolean showAdminMenu() {
        UI.printBox("ADMIN DASHBOARD");
        System.out.println("  " + session.getSessionInfo());
        UI.printLine();
        System.out.println("    [1] View My Profile");
        System.out.println("    [2] Update Profile");
        System.out.println("    [3] Change Password");
        System.out.println("    [4] Add New Student Account");
        System.out.println("    [5] Look Up Student by Email");
        System.out.println("    [0] Logout");
        UI.printLine();
        System.out.print("  Enter choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                session.getCurrentUser().displayProfile();
                pause();
                break;
            case "2":
                profileMgr.updateProfile(session.getCurrentUser());
                pause();
                break;
            case "3":
                profileMgr.changePassword(session.getCurrentUser());
                pause();
                break;
            case "4":
                addStudent();
                pause();
                break;
            case "5":
                lookupStudent();
                pause();
                break;
            case "0":
                loginMgr.logout(session);
                return false;
            default:
                UI.error("Invalid choice. Please try again.");
        }
        return true;
    }

    private void addStudent() {
        UI.printBox("ADD NEW STUDENT");
        System.out.print("  Name        : "); String name = scanner.nextLine().trim();
        System.out.print("  Email       : "); String email = scanner.nextLine().trim();

        if (db.findByEmail(email) != null) {
            UI.error("A user with this email already exists."); return;
        }

        System.out.print("  Password    : "); String pass = scanner.nextLine().trim();
        System.out.print("  Roll Number : "); String roll = scanner.nextLine().trim();
        System.out.print("  Department  : "); String dept = scanner.nextLine().trim();
        System.out.print("  Semester    : "); String sem  = scanner.nextLine().trim();
        System.out.print("  Phone       : "); String phone= scanner.nextLine().trim();
        System.out.print("  DOB (YYYY-MM-DD): "); String dob = scanner.nextLine().trim();

        User newUser = new User(name, email, pass, roll, dept, sem, phone, dob, "student");
        db.addUser(newUser);
        UI.success("Student account created for " + name + ".");
        UI.printLine();
    }

    private void lookupStudent() {
        UI.printBox("STUDENT LOOKUP");
        System.out.print("  Enter student email: ");
        String email = scanner.nextLine().trim();
        User u = db.findByEmail(email);
        if (u != null) u.displayProfile();
        else UI.error("No user found with email: " + email);
    }

    private void pause() {
        System.out.print("\n  Press ENTER to continue...");
        scanner.nextLine();
    }
}

// ─────────────────────────────────────────────────────────────
// MAIN CLASS — Entry point
// ─────────────────────────────────────────────────────────────
public class ExamManagementSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Menu menu = new Menu(scanner);
        menu.run();
        scanner.close();
        System.out.println("\n  Thank you for using ExamPortal. Goodbye!\n");
    }
}
