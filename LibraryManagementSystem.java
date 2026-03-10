import java.util.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 * ================================================================
 *         LIBRANET - LIBRARY MANAGEMENT SYSTEM
 *         Complete Single-File Java Application
 * ================================================================
 *
 * ARCHITECTURE:
 *   Pattern     : MVC (Model-View-Controller)
 *   Persistence : In-Memory Database (ArrayList-based)
 *   UI          : Console-based Menu-Driven Interface
 *
 * MODULES:
 *   1. AuthModule         - Login, Logout, Register, Sessions
 *   2. BookModule         - Add, Update, Delete, Search Books
 *   3. MemberModule       - Add, Update, Delete, Search Members
 *   4. IssuanceModule     - Issue and Return Books
 *   5. ReservationModule  - Advance Book Reservations
 *   6. FineModule         - Fine Calculation and Collection
 *   7. ReportModule       - Analytics and Reports
 *   8. AdminController    - Admin-only operations
 *   9. UserController     - Member-only operations
 *
 * DATABASE TABLES (In-Memory):
 *   - books[]          : Book records
 *   - users[]          : Member/User records
 *   - issuances[]      : Book issue/return transactions
 *   - reservations[]   : Advance booking records
 *   - fines[]          : Fine records
 *   - activityLog[]    : System activity trail
 *
 * RULES:
 *   - Fine Rate     : Rs.2 per day overdue
 *   - Loan Period   : 14 days default
 *   - Max Issues    : 3 books per member
 *   - Max Reserves  : 2 reservations per member
 *
 * HOW TO COMPILE & RUN:
 *   javac LibraryManagementSystem.java
 *   java  LibraryManagementSystem
 *
 * DEFAULT CREDENTIALS:
 *   Admin : admin@library.com  / admin123
 *   User  : user@library.com   / user123
 * ================================================================
 */
public class LibraryManagementSystem {

    // ─────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────
    static final double FINE_RATE    = 2.0;   // Rs per day
    static final int    LOAN_DAYS    = 14;     // default loan period
    static final int    MAX_ISSUES   = 3;      // max books per member
    static final int    MAX_RESERVES = 2;      // max reservations per member
    static final String DATE_FORMAT  = "dd-MM-yyyy";
    static final String LINE         = "═".repeat(70);
    static final String THIN_LINE    = "─".repeat(70);

    // ─────────────────────────────────────────
    //  IN-MEMORY DATABASE
    // ─────────────────────────────────────────
    static List<Book>        books        = new ArrayList<>();
    static List<User>        users        = new ArrayList<>();
    static List<Issuance>    issuances    = new ArrayList<>();
    static List<Reservation> reservations = new ArrayList<>();
    static List<Fine>        fines        = new ArrayList<>();
    static List<String>      activityLog  = new ArrayList<>();

    // ID counters
    static int bookIdCounter        = 0;
    static int userIdCounter        = 0;
    static int issuanceIdCounter    = 0;
    static int reservationIdCounter = 0;
    static int fineIdCounter        = 0;

    // Current session
    static User currentUser = null;
    static Scanner sc = new Scanner(System.in);

    // ─────────────────────────────────────────
    //  MODEL CLASSES
    // ─────────────────────────────────────────

    /** Represents a Book in the library catalog */
    static class Book {
        String id, title, author, isbn, category, publisher, shelf, description;
        int    year, totalCopies, availableCopies;
        int    issueCount;  // for popularity tracking

        Book(String title, String author, String isbn, String category,
             String publisher, int year, int copies, String shelf, String description) {
            this.id              = "BK" + String.format("%03d", ++bookIdCounter);
            this.title           = title;
            this.author          = author;
            this.isbn            = isbn;
            this.category        = category;
            this.publisher       = publisher;
            this.year            = year;
            this.totalCopies     = copies;
            this.availableCopies = copies;
            this.shelf           = shelf;
            this.description     = description;
            this.issueCount      = 0;
        }

        String getStatus() {
            if (availableCopies == 0)           return "UNAVAILABLE";
            if (availableCopies < totalCopies)  return "PARTLY ISSUED";
            return "AVAILABLE";
        }

        @Override
        public String toString() {
            return String.format("%-8s %-30s %-20s %-18s %d/%d  %s",
                id, truncate(title, 28), truncate(author, 18),
                truncate(category, 16), availableCopies, totalCopies, getStatus());
        }
    }

    /** Represents a User (Admin or Member) */
    static class User {
        String  id, name, email, password, phone, role, joinDate;
        boolean active;

        User(String name, String email, String password, String phone, String role) {
            this.id       = "USR" + String.format("%03d", ++userIdCounter);
            this.name     = name;
            this.email    = email;
            this.password = password;
            this.phone    = phone;
            this.role     = role;
            this.joinDate = todayStr();
            this.active   = true;
        }

        boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

        @Override
        public String toString() {
            int active = (int) issuances.stream()
                .filter(i -> i.memberId.equals(id) && !i.status.equals("RETURNED")).count();
            return String.format("%-8s %-22s %-28s %-8s %d  %s",
                id, truncate(name, 20), truncate(email, 26), role, active, joinDate);
        }
    }

    /** Represents a Book Issuance transaction */
    static class Issuance {
        String id, bookId, memberId, issueDate, dueDate, returnDate, status;
        double finePaid;

        Issuance(String bookId, String memberId, String issueDate, String dueDate) {
            this.id         = "ISS" + String.format("%03d", ++issuanceIdCounter);
            this.bookId     = bookId;
            this.memberId   = memberId;
            this.issueDate  = issueDate;
            this.dueDate    = dueDate;
            this.returnDate = null;
            this.status     = "ISSUED";
            this.finePaid   = 0.0;
        }

        /** Auto-update status based on dates */
        void refreshStatus() {
            if (status.equals("RETURNED")) return;
            if (isDateBefore(dueDate, todayStr())) status = "OVERDUE";
            else status = "ISSUED";
        }

        double calculateFine() {
            if (status.equals("RETURNED")) return finePaid;
            String ref = (returnDate != null) ? returnDate : todayStr();
            long days = daysBetween(dueDate, ref);
            return days > 0 ? days * FINE_RATE : 0.0;
        }
    }

    /** Represents a Book Reservation */
    static class Reservation {
        String id, bookId, memberId, reservedDate, expectedDate, status;

        Reservation(String bookId, String memberId, String expectedDate) {
            this.id           = "RES" + String.format("%03d", ++reservationIdCounter);
            this.bookId       = bookId;
            this.memberId     = memberId;
            this.reservedDate = todayStr();
            this.expectedDate = expectedDate;
            this.status       = "PENDING";
        }
    }

    /** Represents a Fine record */
    static class Fine {
        String id, issuanceId, memberId;
        double amount;
        String status, createdDate, paidDate;

        Fine(String issuanceId, String memberId, double amount) {
            this.id          = "FNE" + String.format("%03d", ++fineIdCounter);
            this.issuanceId  = issuanceId;
            this.memberId    = memberId;
            this.amount      = amount;
            this.status      = "PENDING";
            this.createdDate = todayStr();
            this.paidDate    = null;
        }
    }

    // ─────────────────────────────────────────
    //  DATE UTILITY METHODS
    // ─────────────────────────────────────────

    static String todayStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    static String addDays(String dateStr, int days) {
        try {
            LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DATE_FORMAT));
            return d.plusDays(days).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (Exception e) { return dateStr; }
    }

    static long daysBetween(String from, String to) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT);
            LocalDate d1 = LocalDate.parse(from, fmt);
            LocalDate d2 = LocalDate.parse(to,   fmt);
            return ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) { return 0; }
    }

    static boolean isDateBefore(String date1, String date2) {
        return daysBetween(date2, date1) < 0;
    }

    static boolean isValidDate(String d) {
        try {
            LocalDate.parse(d, DateTimeFormatter.ofPattern(DATE_FORMAT));
            return true;
        } catch (Exception e) { return false; }
    }

    // ─────────────────────────────────────────
    //  STRING / DISPLAY UTILITIES
    // ─────────────────────────────────────────

    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    static void printTitle(String title) {
        System.out.println("\n" + LINE);
        System.out.println("  ★  " + title);
        System.out.println(LINE);
    }

    static void printSuccess(String msg) { System.out.println("  ✔  " + msg); }
    static void printError(String msg)   { System.out.println("  ✘  ERROR: " + msg); }
    static void printInfo(String msg)    { System.out.println("  ℹ  " + msg); }
    static void printWarning(String msg) { System.out.println("  ⚠  " + msg); }

    static void pause() {
        System.out.print("\n  [ Press ENTER to continue ]");
        sc.nextLine();
    }

    static String input(String prompt) {
        System.out.print("  " + prompt + ": ");
        return sc.nextLine().trim();
    }

    static int inputInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(input(prompt));
            } catch (NumberFormatException e) {
                printError("Enter a valid number.");
            }
        }
    }

    static double inputDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(input(prompt));
            } catch (NumberFormatException e) {
                printError("Enter a valid number.");
            }
        }
    }

    // ─────────────────────────────────────────
    //  LOOKUP HELPERS
    // ─────────────────────────────────────────

    static Book findBook(String id) {
        return books.stream().filter(b -> b.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    static User findUser(String id) {
        return users.stream().filter(u -> u.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    static Issuance findIssuance(String id) {
        return issuances.stream().filter(i -> i.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    static Reservation findReservation(String id) {
        return reservations.stream().filter(r -> r.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    static long activeIssuesForMember(String memberId) {
        return issuances.stream()
            .filter(i -> i.memberId.equals(memberId) && !i.status.equals("RETURNED"))
            .count();
    }

    static void logActivity(String action) {
        activityLog.add("[" + todayStr() + "] " + action);
        if (activityLog.size() > 100) activityLog.remove(0);
    }

    // ─────────────────────────────────────────
    //  DATA SEEDING (Sample Data)
    // ─────────────────────────────────────────

    static void seedData() {
        // Admin user
        users.add(new User("Admin User",    "admin@library.com", "admin123", "9876543210", "admin"));
        users.add(new User("Arjun Sharma",  "user@library.com",  "user123",  "8765432109", "user"));
        users.add(new User("Priya Nair",    "priya@library.com", "pass123",  "7654321098", "user"));
        users.add(new User("Rahul Verma",   "rahul@library.com", "pass123",  "6543210987", "user"));
        users.add(new User("Sneha Patel",   "sneha@library.com", "pass123",  "5432109876", "user"));

        // Books
        books.add(new Book("Introduction to Algorithms",    "Thomas H. Cormen",     "978-0262033848", "Computer Science", "MIT Press",       2022, 3, "CS-01", "Comprehensive algorithms and data structures."));
        books.add(new Book("Clean Code",                    "Robert C. Martin",     "978-0132350884", "Computer Science", "Prentice Hall",   2008, 2, "CS-02", "A handbook of agile software craftsmanship."));
        books.add(new Book("Design Patterns",               "Gang of Four",         "978-0201633610", "Computer Science", "Addison-Wesley",  1994, 3, "CS-03", "Reusable Object-Oriented Software patterns."));
        books.add(new Book("Python Crash Course",           "Eric Matthes",         "978-1593279288", "Computer Science", "No Starch",       2019, 4, "CS-04", "Hands-on project-based intro to programming."));
        books.add(new Book("The Great Gatsby",              "F. Scott Fitzgerald",  "978-0743273565", "Literature",       "Scribner",        1925, 4, "LIT-01","Classic American novel set in the Jazz Age."));
        books.add(new Book("To Kill a Mockingbird",         "Harper Lee",           "978-0061935466", "Literature",       "Harper",          1960, 3, "LIT-02","Pulitzer Prize-winning novel on racial injustice."));
        books.add(new Book("Calculus: Early Transcendentals","James Stewart",       "978-1285741550", "Mathematics",      "Cengage",         2015, 5, "MAT-01","Essential calculus textbook for university."));
        books.add(new Book("Discrete Mathematics",          "Kenneth Rosen",        "978-0073383095", "Mathematics",      "McGraw-Hill",     2018, 4, "MAT-02","Applications to Computer Science and Math."));
        books.add(new Book("A Brief History of Time",       "Stephen Hawking",      "978-0553380163", "Physics",          "Bantam Books",    1998, 2, "PHY-01","From the Big Bang to Black Holes."));
        books.add(new Book("Sapiens",                       "Yuval Noah Harari",    "978-0062316097", "History",          "Harper",          2015, 3, "HIS-01","A Brief History of Humankind."));
        books.add(new Book("Engineering Mathematics",       "H.K. Dass",            "978-8121919371", "Engineering",      "SChand",          2020, 6, "ENG-01","Comprehensive engineering mathematics."));
        books.add(new Book("Thinking, Fast and Slow",       "Daniel Kahneman",      "978-0374533557", "Philosophy",       "FSG",             2011, 2, "PHI-01","How we think — two systems of thought."));

        // Sample issuances (some overdue for demo)
        Issuance i1 = new Issuance("BK001", "USR002", addDays(todayStr(), -20), addDays(todayStr(), -6));
        i1.refreshStatus();
        issuances.add(i1);
        books.get(0).availableCopies--;
        books.get(0).issueCount++;

        Issuance i2 = new Issuance("BK002", "USR003", addDays(todayStr(), -18), addDays(todayStr(), -4));
        i2.refreshStatus();
        issuances.add(i2);
        books.get(1).availableCopies--;
        books.get(1).issueCount++;

        Issuance i3 = new Issuance("BK010", "USR004", addDays(todayStr(), -5), addDays(todayStr(), 9));
        issuances.add(i3);
        books.get(9).availableCopies--;
        books.get(9).issueCount++;

        Issuance i4 = new Issuance("BK004", "USR002", addDays(todayStr(), -16), addDays(todayStr(), -2));
        i4.status      = "RETURNED";
        i4.returnDate  = addDays(todayStr(), -3);
        i4.finePaid    = 0;
        issuances.add(i4);
        books.get(3).issueCount++;

        // Sample reservation
        reservations.add(new Reservation("BK001", "USR003", addDays(todayStr(), 7)));
        reservations.add(new Reservation("BK002", "USR005", addDays(todayStr(), 5)));

        logActivity("System initialized with sample data.");
        logActivity("Admin logged in for the first time.");
    }

    // ═════════════════════════════════════════
    //  AUTH MODULE
    // ═════════════════════════════════════════

    static void authMenu() {
        while (true) {
            printTitle("LIBRANET — LIBRARY MANAGEMENT SYSTEM");
            System.out.println("  Welcome to the Digital Library System");
            System.out.println(THIN_LINE);
            System.out.println("  1. Login");
            System.out.println("  2. Register as New Member");
            System.out.println("  3. Exit");
            System.out.println(THIN_LINE);
            System.out.println("  Demo Credentials:");
            System.out.println("   Admin : admin@library.com / admin123");
            System.out.println("   User  : user@library.com  / user123");
            System.out.println(LINE);

            String ch = input("Choose option");
            switch (ch) {
                case "1": login();    break;
                case "2": register(); break;
                case "3":
                    printInfo("Thank you for using LibraNet. Goodbye!");
                    System.exit(0);
                default:
                    printError("Invalid option. Try again.");
            }
        }
    }

    static void login() {
        printTitle("LOGIN");
        String email = input("Email");
        String pass  = input("Password");

        Optional<User> found = users.stream()
            .filter(u -> u.email.equalsIgnoreCase(email) && u.password.equals(pass) && u.active)
            .findFirst();

        if (found.isPresent()) {
            currentUser = found.get();
            printSuccess("Welcome back, " + currentUser.name + "! [" + currentUser.role.toUpperCase() + "]");
            logActivity(currentUser.name + " logged in.");
            pause();
            if (currentUser.isAdmin()) adminMenu();
            else userMenu();
        } else {
            printError("Invalid credentials. Check your email and password.");
            pause();
        }
    }

    static void register() {
        printTitle("MEMBER REGISTRATION");
        String name  = input("Full Name");
        String email = input("Email Address");
        String phone = input("Phone Number");
        String pass  = input("Password (min 6 chars)");
        String confP = input("Confirm Password");

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            printError("Name, email and password are required."); pause(); return;
        }
        if (pass.length() < 6) {
            printError("Password must be at least 6 characters."); pause(); return;
        }
        if (!pass.equals(confP)) {
            printError("Passwords do not match."); pause(); return;
        }
        if (users.stream().anyMatch(u -> u.email.equalsIgnoreCase(email))) {
            printError("Email already registered. Please login."); pause(); return;
        }

        User newUser = new User(name, email, pass, phone, "user");
        users.add(newUser);
        logActivity("New member registered: " + name + " (" + newUser.id + ")");
        printSuccess("Account created! Your Member ID: " + newUser.id);
        printInfo("You can now login with your credentials.");
        pause();
    }

    static void logout() {
        logActivity(currentUser.name + " logged out.");
        printInfo("Logged out successfully. Goodbye, " + currentUser.name + "!");
        currentUser = null;
        pause();
    }

    // ═════════════════════════════════════════
    //  ADMIN MENU
    // ═════════════════════════════════════════

    static void adminMenu() {
        while (true) {
            printTitle("ADMIN DASHBOARD — " + currentUser.name);
            // Show quick stats
            long active   = issuances.stream().filter(i -> !i.status.equals("RETURNED")).count();
            long overdue  = issuances.stream().filter(i ->  i.status.equals("OVERDUE")).count();
            long pending  = reservations.stream().filter(r -> r.status.equals("PENDING")).count();
            double fineAmt = issuances.stream().mapToDouble(Issuance::calculateFine).sum();
            System.out.printf("  Books: %-4d  Members: %-4d  Issued: %-4d  Overdue: %-4d  Reservations: %-3d  Pending Fines: Rs.%.1f%n",
                books.size(), users.stream().filter(u->!u.isAdmin()).count(),
                active, overdue, pending, fineAmt);
            System.out.println(THIN_LINE);
            System.out.println("  BOOK MANAGEMENT        MEMBER MANAGEMENT");
            System.out.println("  1. View All Books      7. View All Members");
            System.out.println("  2. Add New Book        8. Add New Member");
            System.out.println("  3. Update Book         9. Update Member");
            System.out.println("  4. Delete Book        10. Delete Member");
            System.out.println("  5. Search Books       11. Search Members");
            System.out.println("  6. View Book Detail");
            System.out.println(THIN_LINE);
            System.out.println("  TRANSACTIONS           SYSTEM");
            System.out.println("  12. Issue Book        18. View Reports");
            System.out.println("  13. Return Book       19. View Activity Log");
            System.out.println("  14. View All Issues   20. Manage Reservations");
            System.out.println("  15. View Overdue      21. My Profile");
            System.out.println("  16. Manage Fines");
            System.out.println("  17. Collect Fine");
            System.out.println(THIN_LINE);
            System.out.println("  0. Logout");
            System.out.println(LINE);

            String ch = input("Choose option");
            switch (ch) {
                case "1":  viewAllBooks();          break;
                case "2":  addBook();               break;
                case "3":  updateBook();            break;
                case "4":  deleteBook();            break;
                case "5":  searchBooks();           break;
                case "6":  viewBookDetail();        break;
                case "7":  viewAllMembers();        break;
                case "8":  addMember();             break;
                case "9":  updateMember();          break;
                case "10": deleteMember();          break;
                case "11": searchMembers();         break;
                case "12": issueBook();             break;
                case "13": returnBook();            break;
                case "14": viewAllIssues();         break;
                case "15": viewOverdueIssues();     break;
                case "16": viewAllFines();          break;
                case "17": collectFine();           break;
                case "18": viewReports();           break;
                case "19": viewActivityLog();       break;
                case "20": manageReservations();    break;
                case "21": viewProfile();           break;
                case "0":  logout(); return;
                default:   printError("Invalid option."); pause();
            }
        }
    }

    // ═════════════════════════════════════════
    //  USER MENU
    // ═════════════════════════════════════════

    static void userMenu() {
        while (true) {
            // Auto-update overdue status
            issuances.forEach(Issuance::refreshStatus);

            long myActive = activeIssuesForMember(currentUser.id);
            double myFines = issuances.stream()
                .filter(i -> i.memberId.equals(currentUser.id))
                .mapToDouble(Issuance::calculateFine).sum();

            printTitle("MEMBER DASHBOARD — " + currentUser.name);
            System.out.printf("  Member ID: %-10s   Active Issues: %-3d   Pending Fines: Rs.%.1f%n",
                currentUser.id, myActive, myFines);
            if (myFines > 0) System.out.println("  ⚠  You have outstanding fines. Please clear them.");
            System.out.println(THIN_LINE);
            System.out.println("  BOOKS                  MY ACCOUNT");
            System.out.println("  1. Browse All Books    7. My Issued Books");
            System.out.println("  2. Search Books        8. My Reservations");
            System.out.println("  3. View Book Detail    9. My Fines");
            System.out.println("  4. Issue a Book       10. Pay Fine");
            System.out.println("  5. Return a Book      11. My Profile");
            System.out.println("  6. Reserve a Book     12. Update Profile");
            System.out.println("  ─────────────────────────────────────");
            System.out.println("  13. Contact Library    0. Logout");
            System.out.println(LINE);

            String ch = input("Choose option");
            switch (ch) {
                case "1":  viewAllBooks();          break;
                case "2":  searchBooks();           break;
                case "3":  viewBookDetail();        break;
                case "4":  userIssueBook();         break;
                case "5":  userReturnBook();        break;
                case "6":  userReserveBook();       break;
                case "7":  viewMyBooks();           break;
                case "8":  viewMyReservations();    break;
                case "9":  viewMyFines();           break;
                case "10": userPayFine();           break;
                case "11": viewProfile();           break;
                case "12": updateProfile();         break;
                case "13": contactLibrary();        break;
                case "0":  logout(); return;
                default:   printError("Invalid option."); pause();
            }
        }
    }

    // ═════════════════════════════════════════
    //  BOOK MODULE
    // ═════════════════════════════════════════

    static void viewAllBooks() {
        printTitle("ALL BOOKS (" + books.size() + " total)");
        System.out.printf("  %-8s %-30s %-20s %-18s %-6s %s%n",
            "ID", "TITLE", "AUTHOR", "CATEGORY", "AVAIL", "STATUS");
        System.out.println(THIN_LINE);
        if (books.isEmpty()) { printInfo("No books found."); }
        else books.forEach(b -> System.out.println("  " + b));
        System.out.println(LINE);
        pause();
    }

    static void addBook() {
        printTitle("ADD NEW BOOK");
        String title    = input("Title");
        String author   = input("Author");
        String isbn     = input("ISBN");

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            printError("Title, author and ISBN are required."); pause(); return;
        }
        if (books.stream().anyMatch(b -> b.isbn.equals(isbn))) {
            printError("A book with this ISBN already exists."); pause(); return;
        }

        System.out.println("  Categories: 1.Computer Science  2.Mathematics  3.Physics  4.Literature");
        System.out.println("              5.History  6.Engineering  7.Arts  8.Philosophy  9.Science");
        String[] cats = {"", "Computer Science","Mathematics","Physics","Literature","History","Engineering","Arts","Philosophy","Science"};
        int catIdx = inputInt("Category number (1-9)");
        String category = (catIdx >= 1 && catIdx <= 9) ? cats[catIdx] : "General";

        String publisher = input("Publisher");
        int year = 0;
        try { year = Integer.parseInt(input("Publication Year")); } catch (Exception ignored) {}
        int copies = 0;
        while (copies < 1) { copies = inputInt("Number of Copies"); if (copies < 1) printError("Must be at least 1."); }
        String shelf = input("Shelf Location (e.g. CS-01)");
        String desc  = input("Description (optional)");

        Book newBook = new Book(title, author, isbn, category, publisher, year, copies, shelf, desc);
        books.add(newBook);
        logActivity("Book added: " + title + " (" + newBook.id + ") by " + currentUser.name);
        printSuccess("Book added successfully! ID: " + newBook.id);
        pause();
    }

    static void updateBook() {
        printTitle("UPDATE BOOK");
        String id = input("Enter Book ID to update");
        Book b = findBook(id);
        if (b == null) { printError("Book not found: " + id); pause(); return; }

        System.out.println("  Current: " + b.title + " by " + b.author);
        System.out.println("  (Press ENTER to keep current value)");
        System.out.println(THIN_LINE);

        String title = input("New Title [" + b.title + "]");
        if (!title.isEmpty()) b.title = title;

        String author = input("New Author [" + b.author + "]");
        if (!author.isEmpty()) b.author = author;

        String publisher = input("New Publisher [" + b.publisher + "]");
        if (!publisher.isEmpty()) b.publisher = publisher;

        String shelf = input("New Shelf Location [" + b.shelf + "]");
        if (!shelf.isEmpty()) b.shelf = shelf;

        String desc = input("New Description [" + b.description + "]");
        if (!desc.isEmpty()) b.description = desc;

        String copiesStr = input("New Total Copies [" + b.totalCopies + "]");
        if (!copiesStr.isEmpty()) {
            try {
                int newCopies = Integer.parseInt(copiesStr);
                int issued = b.totalCopies - b.availableCopies;
                if (newCopies < issued) {
                    printError("Cannot reduce below currently issued copies (" + issued + ")."); pause(); return;
                }
                b.availableCopies = newCopies - issued;
                b.totalCopies     = newCopies;
            } catch (NumberFormatException e) { printError("Invalid number, keeping original."); }
        }

        logActivity("Book updated: " + b.title + " (" + b.id + ") by " + currentUser.name);
        printSuccess("Book updated successfully.");
        pause();
    }

    static void deleteBook() {
        printTitle("DELETE BOOK");
        String id = input("Enter Book ID to delete");
        Book b = findBook(id);
        if (b == null) { printError("Book not found: " + id); pause(); return; }

        if (b.availableCopies < b.totalCopies) {
            printError("Cannot delete: book has active issues. Return all copies first."); pause(); return;
        }

        System.out.println("  Book: " + b.title + " by " + b.author);
        String confirm = input("Type 'YES' to confirm deletion");
        if (!confirm.equals("YES")) { printInfo("Deletion cancelled."); pause(); return; }

        books.remove(b);
        logActivity("Book deleted: " + b.title + " (" + id + ") by " + currentUser.name);
        printSuccess("Book deleted successfully.");
        pause();
    }

    static void searchBooks() {
        printTitle("SEARCH BOOKS");
        System.out.println("  Search by: 1.Title  2.Author  3.Category  4.ISBN");
        String ch = input("Search type");
        String keyword = input("Search keyword").toLowerCase();

        List<Book> results = new ArrayList<>();
        switch (ch) {
            case "1": books.stream().filter(b -> b.title.toLowerCase().contains(keyword)).forEach(results::add); break;
            case "2": books.stream().filter(b -> b.author.toLowerCase().contains(keyword)).forEach(results::add); break;
            case "3": books.stream().filter(b -> b.category.toLowerCase().contains(keyword)).forEach(results::add); break;
            case "4": books.stream().filter(b -> b.isbn.toLowerCase().contains(keyword)).forEach(results::add); break;
            default:  books.stream().filter(b -> b.title.toLowerCase().contains(keyword) || b.author.toLowerCase().contains(keyword)).forEach(results::add);
        }

        System.out.println("\n  Found " + results.size() + " result(s):");
        System.out.println(THIN_LINE);
        System.out.printf("  %-8s %-30s %-20s %-18s %-6s %s%n","ID","TITLE","AUTHOR","CATEGORY","AVAIL","STATUS");
        System.out.println(THIN_LINE);
        if (results.isEmpty()) printInfo("No books found matching '" + keyword + "'.");
        else results.forEach(b -> System.out.println("  " + b));
        System.out.println(LINE);
        pause();
    }

    static void viewBookDetail() {
        printTitle("BOOK DETAILS");
        String id = input("Enter Book ID");
        Book b = findBook(id);
        if (b == null) { printError("Book not found: " + id); pause(); return; }

        System.out.println(THIN_LINE);
        System.out.println("  Book ID      : " + b.id);
        System.out.println("  Title        : " + b.title);
        System.out.println("  Author       : " + b.author);
        System.out.println("  ISBN         : " + b.isbn);
        System.out.println("  Category     : " + b.category);
        System.out.println("  Publisher    : " + b.publisher);
        System.out.println("  Year         : " + (b.year > 0 ? b.year : "N/A"));
        System.out.println("  Total Copies : " + b.totalCopies);
        System.out.println("  Available    : " + b.availableCopies);
        System.out.println("  Shelf        : " + b.shelf);
        System.out.println("  Status       : " + b.getStatus());
        System.out.println("  Times Issued : " + b.issueCount);
        if (b.description != null && !b.description.isEmpty())
            System.out.println("  Description  : " + b.description);

        // Show who has it issued
        List<Issuance> activeForBook = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.bookId.equals(b.id) && !i.status.equals("RETURNED")) activeForBook.add(i);
        }
        if (!activeForBook.isEmpty()) {
            System.out.println(THIN_LINE);
            System.out.println("  Currently Issued To:");
            for (Issuance i : activeForBook) {
                User u = findUser(i.memberId);
                System.out.printf("    %s  %-20s  Issued: %s  Due: %s  [%s]%n",
                    i.id, (u != null ? u.name : "Unknown"), i.issueDate, i.dueDate, i.status);
            }
        }

        // Show pending reservations
        List<Reservation> res = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.bookId.equals(b.id) && r.status.equals("PENDING")) res.add(r);
        }
        if (!res.isEmpty()) {
            System.out.println(THIN_LINE);
            System.out.println("  Pending Reservations:");
            for (Reservation r : res) {
                User u = findUser(r.memberId);
                System.out.printf("    %s  %-20s  Reserved: %s  Expected: %s%n",
                    r.id, (u != null ? u.name : "Unknown"), r.reservedDate, r.expectedDate);
            }
        }
        System.out.println(LINE);
        pause();
    }

    // ═════════════════════════════════════════
    //  MEMBER MODULE
    // ═════════════════════════════════════════

    static void viewAllMembers() {
        printTitle("ALL MEMBERS");
        System.out.printf("  %-8s %-22s %-28s %-8s %-6s %s%n",
            "ID", "NAME", "EMAIL", "ROLE", "ISSUED", "JOINED");
        System.out.println(THIN_LINE);
        if (users.isEmpty()) printInfo("No members found.");
        else users.forEach(u -> System.out.println("  " + u));
        System.out.println(LINE);
        pause();
    }

    static void addMember() {
        printTitle("ADD NEW MEMBER");
        String name  = input("Full Name");
        String email = input("Email Address");
        String phone = input("Phone Number");
        String pass  = input("Password");
        String role  = input("Role (user/admin) [user]");
        if (role.isEmpty()) role = "user";
        if (!role.equals("admin") && !role.equals("user")) role = "user";

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            printError("Name, email and password required."); pause(); return;
        }
        if (users.stream().anyMatch(u -> u.email.equalsIgnoreCase(email))) {
            printError("Email already registered."); pause(); return;
        }

        User newUser = new User(name, email, pass, phone, role);
        users.add(newUser);
        logActivity("Member added: " + name + " (" + newUser.id + ") by " + currentUser.name);
        printSuccess("Member added! ID: " + newUser.id);
        pause();
    }

    static void updateMember() {
        printTitle("UPDATE MEMBER");
        String id = input("Enter Member ID to update");
        User u = findUser(id);
        if (u == null) { printError("Member not found: " + id); pause(); return; }

        System.out.println("  Member: " + u.name + " (" + u.email + ")");
        System.out.println("  (Press ENTER to keep current value)");
        System.out.println(THIN_LINE);

        String name = input("New Name [" + u.name + "]");
        if (!name.isEmpty()) u.name = name;

        String email = input("New Email [" + u.email + "]");
        if (!email.isEmpty()) {
            if (!email.equals(u.email) && users.stream().anyMatch(x -> x.email.equalsIgnoreCase(email))) {
                printError("Email already in use."); pause(); return;
            }
            u.email = email;
        }

        String phone = input("New Phone [" + u.phone + "]");
        if (!phone.isEmpty()) u.phone = phone;

        String pass = input("New Password (leave blank to keep)");
        if (!pass.isEmpty()) {
            if (pass.length() < 6) { printError("Password too short."); pause(); return; }
            u.password = pass;
        }

        logActivity("Member updated: " + u.name + " (" + id + ") by " + currentUser.name);
        printSuccess("Member updated successfully.");
        pause();
    }

    static void deleteMember() {
        printTitle("DELETE MEMBER");
        String id = input("Enter Member ID to delete");
        User u = findUser(id);
        if (u == null) { printError("Member not found: " + id); pause(); return; }
        if (u.id.equals(currentUser.id)) { printError("Cannot delete your own account."); pause(); return; }

        long active = activeIssuesForMember(id);
        if (active > 0) {
            printError("Cannot delete: member has " + active + " active issue(s). Return books first."); pause(); return;
        }

        System.out.println("  Member: " + u.name + " (" + u.email + ")");
        String confirm = input("Type 'YES' to confirm deletion");
        if (!confirm.equals("YES")) { printInfo("Deletion cancelled."); pause(); return; }

        users.remove(u);
        logActivity("Member deleted: " + u.name + " (" + id + ") by " + currentUser.name);
        printSuccess("Member deleted successfully.");
        pause();
    }

    static void searchMembers() {
        printTitle("SEARCH MEMBERS");
        System.out.println("  Search by: 1.Name  2.Email  3.ID");
        String ch = input("Search type");
        String keyword = input("Search keyword").toLowerCase();

        List<User> results = new ArrayList<>();
        switch (ch) {
            case "1": users.stream().filter(u -> u.name.toLowerCase().contains(keyword)).forEach(results::add); break;
            case "2": users.stream().filter(u -> u.email.toLowerCase().contains(keyword)).forEach(results::add); break;
            case "3": users.stream().filter(u -> u.id.toLowerCase().contains(keyword)).forEach(results::add); break;
            default:  users.stream().filter(u -> u.name.toLowerCase().contains(keyword)).forEach(results::add);
        }

        System.out.println("\n  Found " + results.size() + " result(s):");
        System.out.printf("  %-8s %-22s %-28s %-8s %-6s %s%n","ID","NAME","EMAIL","ROLE","ISSUED","JOINED");
        System.out.println(THIN_LINE);
        if (results.isEmpty()) printInfo("No members found.");
        else results.forEach(u -> System.out.println("  " + u));
        pause();
    }

    // ═════════════════════════════════════════
    //  ISSUANCE MODULE
    // ═════════════════════════════════════════

    static void issueBook() {
        printTitle("ISSUE BOOK TO MEMBER");

        // Select member
        String memberId = input("Enter Member ID");
        User member = findUser(memberId);
        if (member == null) { printError("Member not found: " + memberId); pause(); return; }
        if (member.isAdmin()) { printError("Cannot issue books to admin accounts."); pause(); return; }

        long active = activeIssuesForMember(memberId);
        if (active >= MAX_ISSUES) {
            printError(member.name + " already has " + active + "/" + MAX_ISSUES + " books. Cannot issue more."); pause(); return;
        }

        // Check pending fines
        double memberFines = issuances.stream()
            .filter(i -> i.memberId.equals(memberId) && !i.status.equals("RETURNED"))
            .mapToDouble(Issuance::calculateFine).sum();
        if (memberFines > 0) {
            printWarning(member.name + " has pending fines of Rs." + String.format("%.1f", memberFines));
            String proceed = input("Proceed anyway? (yes/no)");
            if (!proceed.equalsIgnoreCase("yes")) { printInfo("Issue cancelled."); pause(); return; }
        }

        // Select book
        String bookId = input("Enter Book ID");
        Book book = findBook(bookId);
        if (book == null) { printError("Book not found: " + bookId); pause(); return; }
        if (book.availableCopies <= 0) {
            printError("No copies available for '" + book.title + "'."); pause(); return;
        }

        // Check if already issued this book
        boolean alreadyHas = issuances.stream().anyMatch(
            i -> i.bookId.equals(bookId) && i.memberId.equals(memberId) && !i.status.equals("RETURNED"));
        if (alreadyHas) {
            printError(member.name + " already has this book issued."); pause(); return;
        }

        // Dates
        String issueDate = input("Issue Date [" + todayStr() + "] (dd-MM-yyyy)");
        if (issueDate.isEmpty()) issueDate = todayStr();
        if (!isValidDate(issueDate)) { printError("Invalid date format."); pause(); return; }

        String dueDate = addDays(issueDate, LOAN_DAYS);
        String customDue = input("Due Date [" + dueDate + "] (dd-MM-yyyy, ENTER for default)");
        if (!customDue.isEmpty()) {
            if (!isValidDate(customDue)) { printError("Invalid date format."); pause(); return; }
            dueDate = customDue;
        }

        // Confirm
        System.out.println(THIN_LINE);
        System.out.println("  Member  : " + member.name + " (" + member.id + ")");
        System.out.println("  Book    : " + book.title + " (" + book.id + ")");
        System.out.println("  Issued  : " + issueDate);
        System.out.println("  Due     : " + dueDate);
        System.out.println(THIN_LINE);
        String confirm = input("Confirm issue? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Issue cancelled."); pause(); return; }

        Issuance iss = new Issuance(bookId, memberId, issueDate, dueDate);
        issuances.add(iss);
        book.availableCopies--;
        book.issueCount++;

        // Cancel matching reservation if any
        reservations.stream()
            .filter(r -> r.bookId.equals(bookId) && r.memberId.equals(memberId) && r.status.equals("PENDING"))
            .findFirst().ifPresent(r -> { r.status = "FULFILLED"; });

        logActivity("Book issued: " + book.title + " to " + member.name + " (Issue ID: " + iss.id + ")");
        printSuccess("Book issued successfully!");
        System.out.println("  Issue ID  : " + iss.id);
        System.out.println("  Due Date  : " + iss.dueDate);
        pause();
    }

    static void returnBook() {
        printTitle("RETURN BOOK");

        // List active issues
        List<Issuance> active = new ArrayList<>();
        for (Issuance i : issuances) {
            if (!i.status.equals("RETURNED")) active.add(i);
        }

        if (active.isEmpty()) { printInfo("No active issues to return."); pause(); return; }

        System.out.printf("  %-8s %-30s %-20s %-12s %-12s %s%n",
            "ID", "BOOK", "MEMBER", "ISSUE DATE", "DUE DATE", "STATUS");
        System.out.println(THIN_LINE);
        active.forEach(i -> {
            Book b = findBook(i.bookId); User u = findUser(i.memberId);
            i.refreshStatus();
            System.out.printf("  %-8s %-30s %-20s %-12s %-12s %s%n",
                i.id, truncate(b != null ? b.title : "?", 28),
                truncate(u != null ? u.name  : "?", 18),
                i.issueDate, i.dueDate, i.status);
        });
        System.out.println(LINE);

        String issId = input("Enter Issue ID to return");
        Issuance iss = findIssuance(issId);
        if (iss == null || iss.status.equals("RETURNED")) {
            printError("Issue record not found or already returned."); pause(); return;
        }

        Book book = findBook(iss.bookId);
        User member = findUser(iss.memberId);

        String returnDate = input("Return Date [" + todayStr() + "] (dd-MM-yyyy)");
        if (returnDate.isEmpty()) returnDate = todayStr();
        if (!isValidDate(returnDate)) { printError("Invalid date format."); pause(); return; }

        // Calculate fine
        long overdueDays = daysBetween(iss.dueDate, returnDate);
        double fine = overdueDays > 0 ? overdueDays * FINE_RATE : 0.0;

        System.out.println(THIN_LINE);
        System.out.println("  Book         : " + (book != null ? book.title : iss.bookId));
        System.out.println("  Member       : " + (member != null ? member.name : iss.memberId));
        System.out.println("  Issue Date   : " + iss.issueDate);
        System.out.println("  Due Date     : " + iss.dueDate);
        System.out.println("  Return Date  : " + returnDate);
        if (overdueDays > 0) {
            System.out.printf("  Overdue Days : %d days%n", overdueDays);
            System.out.printf("  Fine Amount  : Rs.%.1f (%d x Rs.%.1f/day)%n", fine, overdueDays, FINE_RATE);
        } else {
            System.out.println("  Fine         : None (Returned on time)");
        }
        System.out.println(THIN_LINE);

        String confirm = input("Confirm return? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Return cancelled."); pause(); return; }

        iss.returnDate = returnDate;
        iss.status     = "RETURNED";
        iss.finePaid   = fine;
        if (book != null) book.availableCopies++;

        // Notify reservations
        if (book != null) {
            reservations.stream()
                .filter(r -> r.bookId.equals(book.id) && r.status.equals("PENDING"))
                .findFirst()
                .ifPresent(r -> {
                    r.status = "AVAILABLE";
                    User ru = findUser(r.memberId);
                    System.out.println("  ★ NOTICE: '" + book.title + "' is now available for reservation by "
                        + (ru != null ? ru.name : r.memberId) + " (Res. " + r.id + ")");
                });
        }

        // Create fine record if applicable
        if (fine > 0) {
            Fine fineRec = new Fine(iss.id, iss.memberId, fine);
            fineRec.status   = "PAID";
            fineRec.paidDate = returnDate;
            fines.add(fineRec);
        }

        logActivity("Book returned: " + (book != null ? book.title : iss.bookId)
            + " by " + (member != null ? member.name : iss.memberId)
            + (fine > 0 ? " | Fine: Rs." + String.format("%.1f", fine) : ""));
        printSuccess("Book returned successfully!");
        if (fine > 0) printInfo("Fine collected: Rs." + String.format("%.1f", fine));
        pause();
    }

    static void viewAllIssues() {
        printTitle("ALL BOOK ISSUES");
        issuances.forEach(Issuance::refreshStatus);

        System.out.println("  Filter: 1.All  2.Active (Issued+Overdue)  3.Overdue  4.Returned");
        String filter = input("Filter");

        List<Issuance> filtered = new ArrayList<>();
        switch (filter) {
            case "2": issuances.stream().filter(i -> !i.status.equals("RETURNED")).forEach(filtered::add); break;
            case "3": issuances.stream().filter(i -> i.status.equals("OVERDUE")).forEach(filtered::add); break;
            case "4": issuances.stream().filter(i -> i.status.equals("RETURNED")).forEach(filtered::add); break;
            default:  filtered.addAll(issuances);
        }

        System.out.printf("%n  %-8s %-28s %-18s %-12s %-12s %-10s %s%n",
            "ID", "BOOK", "MEMBER", "ISSUE DATE", "DUE DATE", "STATUS", "FINE");
        System.out.println(THIN_LINE);
        if (filtered.isEmpty()) { printInfo("No records found."); }
        else {
            filtered.forEach(i -> {
                Book b = findBook(i.bookId); User u = findUser(i.memberId);
                double fine = i.calculateFine();
                System.out.printf("  %-8s %-28s %-18s %-12s %-12s %-10s %s%n",
                    i.id,
                    truncate(b != null ? b.title : i.bookId, 26),
                    truncate(u != null ? u.name  : i.memberId, 16),
                    i.issueDate, i.dueDate, i.status,
                    fine > 0 ? "Rs." + String.format("%.1f", fine) : "—");
            });
        }
        System.out.println(LINE);
        pause();
    }

    static void viewOverdueIssues() {
        printTitle("OVERDUE BOOKS");
        issuances.forEach(Issuance::refreshStatus);

        List<Issuance> overdue = new ArrayList<>();
        for (Issuance i : issuances) {
            if (!i.status.equals("RETURNED") && isDateBefore(i.dueDate, todayStr())) overdue.add(i);
        }

        if (overdue.isEmpty()) { printSuccess("No overdue books! All members are on time."); pause(); return; }

        System.out.printf("  %-8s %-28s %-18s %-12s %-8s %s%n",
            "ID", "BOOK", "MEMBER", "DUE DATE", "DAYS OVR", "FINE");
        System.out.println(THIN_LINE);
        for (Issuance i : overdue) {
            Book b  = findBook(i.bookId);
            User u  = findUser(i.memberId);
            long days = daysBetween(i.dueDate, todayStr());
            double fine = days * FINE_RATE;
            System.out.printf("  %-8s %-28s %-18s %-12s %-8d Rs.%.1f%n",
                i.id,
                truncate(b != null ? b.title : i.bookId, 26),
                truncate(u != null ? u.name  : i.memberId, 16),
                i.dueDate, days, fine);
        }
        double totalFines = overdue.stream().mapToDouble(i -> daysBetween(i.dueDate, todayStr()) * FINE_RATE).sum();
        System.out.println(THIN_LINE);
        System.out.printf("  Total Overdue: %d books   Total Fines: Rs.%.1f%n", overdue.size(), totalFines);
        System.out.println(LINE);
        pause();
    }

    // ═════════════════════════════════════════
    //  RESERVATION MODULE
    // ═════════════════════════════════════════

    static void manageReservations() {
        while (true) {
            printTitle("RESERVATION MANAGEMENT");
            System.out.println("  1. View All Reservations");
            System.out.println("  2. Fulfill Reservation (convert to issue)");
            System.out.println("  3. Cancel Reservation");
            System.out.println("  4. Back");
            System.out.println(LINE);
            String ch = input("Choose option");

            switch (ch) {
                case "1": viewAllReservations(); break;
                case "2": fulfillReservation();  break;
                case "3": cancelReservationAdmin(); break;
                case "4": return;
                default:  printError("Invalid option."); pause();
            }
        }
    }

    static void viewAllReservations() {
        printTitle("ALL RESERVATIONS (" + reservations.size() + " total)");
        System.out.printf("  %-8s %-28s %-18s %-12s %-12s %s%n",
            "ID", "BOOK", "MEMBER", "RESERVED", "EXPECTED", "STATUS");
        System.out.println(THIN_LINE);
        if (reservations.isEmpty()) printInfo("No reservations found.");
        else reservations.forEach(r -> {
            Book b = findBook(r.bookId); User u = findUser(r.memberId);
            System.out.printf("  %-8s %-28s %-18s %-12s %-12s %s%n",
                r.id,
                truncate(b != null ? b.title : r.bookId, 26),
                truncate(u != null ? u.name  : r.memberId, 16),
                r.reservedDate, r.expectedDate, r.status);
        });
        System.out.println(LINE);
        pause();
    }

    static void fulfillReservation() {
        printTitle("FULFILL RESERVATION");
        String resId = input("Enter Reservation ID");
        Reservation res = findReservation(resId);
        if (res == null) { printError("Reservation not found."); pause(); return; }
        if (!res.status.equals("PENDING") && !res.status.equals("AVAILABLE")) {
            printError("Reservation is already " + res.status); pause(); return;
        }

        Book book = findBook(res.bookId);
        User user = findUser(res.memberId);
        if (book == null || user == null) { printError("Book or member not found."); pause(); return; }

        if (book.availableCopies <= 0) {
            printError("No copies available yet for '" + book.title + "'."); pause(); return;
        }

        String dueDate = addDays(todayStr(), LOAN_DAYS);
        Issuance iss = new Issuance(res.bookId, res.memberId, todayStr(), dueDate);
        issuances.add(iss);
        book.availableCopies--;
        book.issueCount++;
        res.status = "FULFILLED";

        logActivity("Reservation " + resId + " fulfilled — " + book.title + " issued to " + user.name);
        printSuccess("Reservation fulfilled! Issue ID: " + iss.id + "  Due: " + dueDate);
        pause();
    }

    static void cancelReservationAdmin() {
        printTitle("CANCEL RESERVATION");
        String resId = input("Enter Reservation ID");
        Reservation res = findReservation(resId);
        if (res == null) { printError("Reservation not found."); pause(); return; }
        if (!res.status.equals("PENDING")) {
            printError("Only PENDING reservations can be cancelled."); pause(); return;
        }
        res.status = "CANCELLED";
        logActivity("Reservation " + resId + " cancelled by admin.");
        printSuccess("Reservation cancelled.");
        pause();
    }

    static void userReserveBook() {
        printTitle("RESERVE A BOOK");

        long myPending = reservations.stream()
            .filter(r -> r.memberId.equals(currentUser.id) && r.status.equals("PENDING")).count();
        if (myPending >= MAX_RESERVES) {
            printError("You already have " + MAX_RESERVES + " pending reservations (maximum allowed)."); pause(); return;
        }

        String bookId = input("Enter Book ID to reserve");
        Book book = findBook(bookId);
        if (book == null) { printError("Book not found: " + bookId); pause(); return; }

        // Check if already reserved
        boolean alreadyRes = reservations.stream().anyMatch(
            r -> r.bookId.equals(bookId) && r.memberId.equals(currentUser.id) && r.status.equals("PENDING"));
        if (alreadyRes) { printError("You have already reserved this book."); pause(); return; }

        // Check if already issued to this user
        boolean alreadyIssued = issuances.stream().anyMatch(
            i -> i.bookId.equals(bookId) && i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED"));
        if (alreadyIssued) { printError("You already have this book issued."); pause(); return; }

        System.out.println("  Book  : " + book.title);
        System.out.println("  Status: " + book.getStatus());
        if (book.availableCopies > 0) {
            printWarning("This book is currently available! Consider issuing it directly.");
            String proceed = input("Reserve anyway? (yes/no)");
            if (!proceed.equalsIgnoreCase("yes")) { printInfo("Reservation cancelled."); pause(); return; }
        }

        String expectedDate = addDays(todayStr(), 7);
        String customDate = input("Expected Available Date [" + expectedDate + "] (dd-MM-yyyy)");
        if (!customDate.isEmpty() && isValidDate(customDate)) expectedDate = customDate;

        Reservation res = new Reservation(bookId, currentUser.id, expectedDate);
        reservations.add(res);
        logActivity(currentUser.name + " reserved '" + book.title + "' (Res. " + res.id + ")");
        printSuccess("Book reserved successfully!");
        System.out.println("  Reservation ID  : " + res.id);
        System.out.println("  Book            : " + book.title);
        System.out.println("  Expected Date   : " + expectedDate);
        pause();
    }

    static void viewMyReservations() {
        printTitle("MY RESERVATIONS");
        List<Reservation> myRes = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.memberId.equals(currentUser.id)) myRes.add(r);
        }

        if (myRes.isEmpty()) { printInfo("You have no reservations."); pause(); return; }

        System.out.printf("  %-8s %-30s %-12s %-12s %s%n","ID","BOOK","RESERVED","EXPECTED","STATUS");
        System.out.println(THIN_LINE);
        myRes.forEach(r -> {
            Book b = findBook(r.bookId);
            System.out.printf("  %-8s %-30s %-12s %-12s %s%n",
                r.id, truncate(b != null ? b.title : r.bookId, 28),
                r.reservedDate, r.expectedDate, r.status);
        });
        System.out.println(THIN_LINE);

        String cancel = input("Enter Reservation ID to cancel (or ENTER to skip)");
        if (!cancel.isEmpty()) {
            Reservation res = findReservation(cancel);
            if (res != null && res.memberId.equals(currentUser.id) && res.status.equals("PENDING")) {
                res.status = "CANCELLED";
                printSuccess("Reservation cancelled.");
            } else {
                printError("Cannot cancel this reservation.");
            }
        }
        pause();
    }

    // ═════════════════════════════════════════
    //  FINE MODULE
    // ═════════════════════════════════════════

    static void viewAllFines() {
        printTitle("FINE MANAGEMENT");
        issuances.forEach(Issuance::refreshStatus);

        List<Issuance> withFines = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.calculateFine() > 0 || (i.status.equals("RETURNED") && i.finePaid > 0)) {
                withFines.add(i);
            }
        }

        // Totals
        double totalPending   = issuances.stream()
            .filter(i -> !i.status.equals("RETURNED")).mapToDouble(Issuance::calculateFine).sum();
        double totalCollected = issuances.stream()
            .filter(i -> i.status.equals("RETURNED")).mapToDouble(i -> i.finePaid).sum();

        System.out.printf("  Total Pending  : Rs.%.1f%n", totalPending);
        System.out.printf("  Total Collected: Rs.%.1f%n", totalCollected);
        System.out.printf("  Grand Total    : Rs.%.1f%n", totalPending + totalCollected);
        System.out.println(THIN_LINE);

        System.out.printf("  %-8s %-18s %-28s %-12s %-8s %s%n",
            "ISS ID", "MEMBER", "BOOK", "DUE DATE", "OVR DAYS", "FINE");
        System.out.println(THIN_LINE);

        if (withFines.isEmpty()) {
            printSuccess("No outstanding fines.");
        } else {
            withFines.forEach(i -> {
                Book b = findBook(i.bookId); User u = findUser(i.memberId);
                long days = i.status.equals("RETURNED") ? 0 : daysBetween(i.dueDate, todayStr());
                double fine = i.calculateFine();
                String status = i.status.equals("RETURNED") ? "PAID" : "PENDING";
                System.out.printf("  %-8s %-18s %-28s %-12s %-8s Rs.%.1f [%s]%n",
                    i.id,
                    truncate(u != null ? u.name : i.memberId, 16),
                    truncate(b != null ? b.title : i.bookId, 26),
                    i.dueDate,
                    days > 0 ? days : "—",
                    fine, status);
            });
        }
        System.out.println(LINE);
        pause();
    }

    static void collectFine() {
        printTitle("COLLECT FINE");
        viewOverdueIssues();

        String issId = input("Enter Issue ID to collect fine for");
        Issuance iss = findIssuance(issId);
        if (iss == null) { printError("Issue not found."); pause(); return; }
        if (iss.status.equals("RETURNED")) { printError("This issue is already returned."); pause(); return; }

        double fine = iss.calculateFine();
        if (fine <= 0) { printInfo("No fine applicable for this issue."); pause(); return; }

        Book b = findBook(iss.bookId); User u = findUser(iss.memberId);
        System.out.println("  Member : " + (u != null ? u.name : iss.memberId));
        System.out.println("  Book   : " + (b != null ? b.title : iss.bookId));
        System.out.printf("  Fine   : Rs.%.1f%n", fine);

        String confirm = input("Collect Rs." + String.format("%.1f", fine) + "? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Collection cancelled."); pause(); return; }

        Fine fineRec = new Fine(iss.id, iss.memberId, fine);
        fineRec.status   = "PAID";
        fineRec.paidDate = todayStr();
        fines.add(fineRec);
        iss.finePaid = fine;

        logActivity("Fine Rs." + String.format("%.1f", fine) + " collected from "
            + (u != null ? u.name : iss.memberId) + " by " + currentUser.name);
        printSuccess("Fine collected: Rs." + String.format("%.1f", fine));
        pause();
    }

    static void viewMyFines() {
        printTitle("MY FINES");
        List<Issuance> myAll = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.memberId.equals(currentUser.id)) myAll.add(i);
        }

        System.out.printf("  %-8s %-28s %-12s %-12s %-12s %-8s %s%n",
            "ISS ID", "BOOK", "ISSUE DATE", "DUE DATE", "RET. DATE", "FINE", "STATUS");
        System.out.println(THIN_LINE);

        double totalPending = 0;
        boolean any = false;
        for (Issuance i : myAll) {
            double fine = i.calculateFine();
            if (fine <= 0 && !i.status.equals("OVERDUE")) continue;
            any = true;
            Book b = findBook(i.bookId);
            String fineStatus = i.status.equals("RETURNED") ? "PAID" : "PENDING";
            if (!i.status.equals("RETURNED")) totalPending += fine;
            System.out.printf("  %-8s %-28s %-12s %-12s %-12s Rs.%-6.1f %s%n",
                i.id,
                truncate(b != null ? b.title : i.bookId, 26),
                i.issueDate, i.dueDate,
                i.returnDate != null ? i.returnDate : "—",
                fine, fineStatus);
        }
        if (!any) { printSuccess("No fines! You're all clear."); }
        else {
            System.out.println(THIN_LINE);
            System.out.printf("  Total Pending Fine: Rs.%.1f%n", totalPending);
        }
        System.out.println(LINE);
        pause();
    }

    static void userPayFine() {
        printTitle("PAY FINE");
        viewMyFines();

        List<Issuance> withFine = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED") && i.calculateFine() > 0)
                withFine.add(i);
        }
        if (withFine.isEmpty()) { printSuccess("No pending fines to pay."); pause(); return; }

        String issId = input("Enter Issue ID to pay fine for");
        Issuance iss = issuances.stream()
            .filter(i -> i.id.equalsIgnoreCase(issId) && i.memberId.equals(currentUser.id)).findFirst().orElse(null);
        if (iss == null) { printError("Issue record not found."); pause(); return; }

        double fine = iss.calculateFine();
        if (fine <= 0) { printInfo("No fine applicable."); pause(); return; }

        System.out.printf("  Fine Amount: Rs.%.1f%n", fine);
        String confirm = input("Pay Rs." + String.format("%.1f", fine) + "? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Payment cancelled."); pause(); return; }

        Fine fineRec = new Fine(iss.id, iss.memberId, fine);
        fineRec.status   = "PAID";
        fineRec.paidDate = todayStr();
        fines.add(fineRec);
        iss.finePaid = fine;

        logActivity(currentUser.name + " paid fine Rs." + String.format("%.1f", fine));
        printSuccess("Fine paid successfully: Rs." + String.format("%.1f", fine));
        pause();
    }

    // ═════════════════════════════════════════
    //  REPORT MODULE
    // ═════════════════════════════════════════

    static void viewReports() {
        while (true) {
            printTitle("REPORTS & ANALYTICS");
            System.out.println("  1. Library Summary Report");
            System.out.println("  2. Books by Category");
            System.out.println("  3. Most Popular Books");
            System.out.println("  4. Issuance Statistics");
            System.out.println("  5. Fine Collection Report");
            System.out.println("  6. Member Activity Report");
            System.out.println("  7. Back");
            System.out.println(LINE);
            String ch = input("Choose report");
            switch (ch) {
                case "1": reportSummary();         break;
                case "2": reportByCategory();      break;
                case "3": reportPopularBooks();    break;
                case "4": reportIssuanceStats();   break;
                case "5": reportFineCollection();  break;
                case "6": reportMemberActivity();  break;
                case "7": return;
                default:  printError("Invalid option."); pause();
            }
        }
    }

    static void reportSummary() {
        printTitle("LIBRARY SUMMARY REPORT — " + todayStr());
        issuances.forEach(Issuance::refreshStatus);

        long totalBooks   = books.stream().mapToLong(b -> b.totalCopies).sum();
        long availBooks   = books.stream().mapToLong(b -> b.availableCopies).sum();
        long members      = users.stream().filter(u -> !u.isAdmin()).count();
        long active       = issuances.stream().filter(i -> !i.status.equals("RETURNED")).count();
        long overdue      = issuances.stream().filter(i -> i.status.equals("OVERDUE")).count();
        long returned     = issuances.stream().filter(i -> i.status.equals("RETURNED")).count();
        long pendingRes   = reservations.stream().filter(r -> r.status.equals("PENDING")).count();
        double pendFines  = issuances.stream().filter(i -> !i.status.equals("RETURNED")).mapToDouble(Issuance::calculateFine).sum();
        double collFines  = issuances.stream().filter(i -> i.status.equals("RETURNED")).mapToDouble(i -> i.finePaid).sum();

        System.out.println(THIN_LINE);
        System.out.printf("  %-35s %s%n", "CATEGORY", "VALUE");
        System.out.println(THIN_LINE);
        System.out.printf("  %-35s %d%n",    "Total Book Titles",          books.size());
        System.out.printf("  %-35s %d%n",    "Total Book Copies",          totalBooks);
        System.out.printf("  %-35s %d%n",    "Available Copies",           availBooks);
        System.out.printf("  %-35s %d%n",    "Copies Currently Issued",    totalBooks - availBooks);
        System.out.println(THIN_LINE);
        System.out.printf("  %-35s %d%n",    "Total Members",              members);
        System.out.println(THIN_LINE);
        System.out.printf("  %-35s %d%n",    "Total Issuances (All Time)", issuances.size());
        System.out.printf("  %-35s %d%n",    "Active Issues",              active);
        System.out.printf("  %-35s %d%n",    "Overdue Issues",             overdue);
        System.out.printf("  %-35s %d%n",    "Returned Issues",            returned);
        System.out.println(THIN_LINE);
        System.out.printf("  %-35s %d%n",    "Pending Reservations",       pendingRes);
        System.out.println(THIN_LINE);
        System.out.printf("  %-35s Rs.%.1f%n","Pending Fines",              pendFines);
        System.out.printf("  %-35s Rs.%.1f%n","Collected Fines",            collFines);
        System.out.printf("  %-35s Rs.%.1f%n","Total Fines Generated",      pendFines + collFines);
        System.out.println(LINE);
        pause();
    }

    static void reportByCategory() {
        printTitle("BOOKS BY CATEGORY");
        Map<String, int[]> catMap = new LinkedHashMap<>();
        for (Book b : books) {
            catMap.computeIfAbsent(b.category, k -> new int[]{0, 0, 0});
            catMap.get(b.category)[0]++;                         // titles
            catMap.get(b.category)[1] += b.totalCopies;         // copies
            catMap.get(b.category)[2] += b.issueCount;          // issues
        }

        System.out.printf("  %-22s %-8s %-8s %-8s %s%n","CATEGORY","TITLES","COPIES","ISSUED","BAR");
        System.out.println(THIN_LINE);
        catMap.forEach((cat, v) -> {
            int barLen = Math.min(v[0] * 3, 25);
            String bar = "█".repeat(barLen);
            System.out.printf("  %-22s %-8d %-8d %-8d %s%n", cat, v[0], v[1], v[2], bar);
        });
        System.out.println(LINE);
        pause();
    }

    static void reportPopularBooks() {
        printTitle("MOST POPULAR BOOKS (by issue count)");
        List<Book> sorted = new ArrayList<>(books);
        sorted.sort((a, b) -> b.issueCount - a.issueCount);

        System.out.printf("  %-4s %-8s %-32s %-18s %s%n","RANK","ID","TITLE","AUTHOR","ISSUES");
        System.out.println(THIN_LINE);
        int rank = 1;
        for (Book b : sorted) {
            if (b.issueCount == 0) continue;
            String bar = "★".repeat(Math.min(b.issueCount, 10));
            System.out.printf("  %-4d %-8s %-32s %-18s %d  %s%n",
                rank++, b.id, truncate(b.title, 30), truncate(b.author, 16), b.issueCount, bar);
        }
        System.out.println(LINE);
        pause();
    }

    static void reportIssuanceStats() {
        printTitle("ISSUANCE STATISTICS");
        issuances.forEach(Issuance::refreshStatus);

        long total    = issuances.size();
        long issued   = issuances.stream().filter(i -> i.status.equals("ISSUED")).count();
        long overdue  = issuances.stream().filter(i -> i.status.equals("OVERDUE")).count();
        long returned = issuances.stream().filter(i -> i.status.equals("RETURNED")).count();

        System.out.printf("  Total Transactions  : %d%n", total);
        System.out.printf("  Active (On Time)    : %d (%.1f%%)%n", issued,   total > 0 ? issued * 100.0 / total : 0);
        System.out.printf("  Overdue             : %d (%.1f%%)%n", overdue,  total > 0 ? overdue * 100.0 / total : 0);
        System.out.printf("  Returned            : %d (%.1f%%)%n", returned, total > 0 ? returned * 100.0 / total : 0);
        System.out.println(THIN_LINE);

        System.out.printf("  %-20s %-6s %-6s %-6s %s%n","MEMBER","ISSUED","OVR","RET","FINES");
        System.out.println(THIN_LINE);
        for (User u : users) {
            if (u.isAdmin()) continue;
            long uIssued   = issuances.stream().filter(i -> i.memberId.equals(u.id) && i.status.equals("ISSUED")).count();
            long uOverdue  = issuances.stream().filter(i -> i.memberId.equals(u.id) && i.status.equals("OVERDUE")).count();
            long uReturned = issuances.stream().filter(i -> i.memberId.equals(u.id) && i.status.equals("RETURNED")).count();
            double uFines  = issuances.stream().filter(i -> i.memberId.equals(u.id)).mapToDouble(Issuance::calculateFine).sum();
            if (uIssued + uOverdue + uReturned == 0) continue;
            System.out.printf("  %-20s %-6d %-6d %-6d Rs.%.1f%n",
                truncate(u.name, 18), uIssued, uOverdue, uReturned, uFines);
        }
        System.out.println(LINE);
        pause();
    }

    static void reportFineCollection() {
        printTitle("FINE COLLECTION REPORT");
        double pendFines = issuances.stream().filter(i -> !i.status.equals("RETURNED")).mapToDouble(Issuance::calculateFine).sum();
        double collFines = issuances.stream().filter(i -> i.status.equals("RETURNED")).mapToDouble(i -> i.finePaid).sum();

        System.out.printf("  Pending Fines  : Rs.%.1f%n", pendFines);
        System.out.printf("  Collected Fines: Rs.%.1f%n", collFines);
        System.out.printf("  Total Generated: Rs.%.1f%n", pendFines + collFines);
        System.out.println(THIN_LINE);

        System.out.printf("  %-8s %-18s %-28s %-12s %-8s %s%n",
            "ISS ID","MEMBER","BOOK","DUE DATE","AMOUNT","STATUS");
        System.out.println(THIN_LINE);

        for (Issuance i : issuances) {
            double fine = i.calculateFine();
            if (fine <= 0) continue;
            Book b = findBook(i.bookId); User u = findUser(i.memberId);
            String st = i.status.equals("RETURNED") ? "PAID" : "PENDING";
            System.out.printf("  %-8s %-18s %-28s %-12s Rs.%-6.1f %s%n",
                i.id,
                truncate(u != null ? u.name : i.memberId, 16),
                truncate(b != null ? b.title : i.bookId, 26),
                i.dueDate, fine, st);
        }
        System.out.println(LINE);
        pause();
    }

    static void reportMemberActivity() {
        printTitle("MEMBER ACTIVITY REPORT");
        System.out.printf("  %-8s %-20s %-20s %-6s %-6s %-8s%n",
            "ID","NAME","EMAIL","ACTIVE","TOTAL","FINES");
        System.out.println(THIN_LINE);
        for (User u : users) {
            if (u.isAdmin()) continue;
            long active = issuances.stream().filter(i -> i.memberId.equals(u.id) && !i.status.equals("RETURNED")).count();
            long total  = issuances.stream().filter(i -> i.memberId.equals(u.id)).count();
            double fine = issuances.stream().filter(i -> i.memberId.equals(u.id)).mapToDouble(Issuance::calculateFine).sum();
            System.out.printf("  %-8s %-20s %-20s %-6d %-6d Rs.%.1f%n",
                u.id, truncate(u.name, 18), truncate(u.email, 18), active, total, fine);
        }
        System.out.println(LINE);
        pause();
    }

    // ═════════════════════════════════════════
    //  ACTIVITY LOG
    // ═════════════════════════════════════════

    static void viewActivityLog() {
        printTitle("SYSTEM ACTIVITY LOG (Last " + Math.min(activityLog.size(), 30) + " entries)");
        if (activityLog.isEmpty()) { printInfo("No activity recorded."); }
        else {
            List<String> log = activityLog;
            int start = Math.max(0, log.size() - 30);
            for (int i = log.size() - 1; i >= start; i--) {
                System.out.println("  " + log.get(i));
            }
        }
        System.out.println(LINE);
        pause();
    }

    // ═════════════════════════════════════════
    //  USER SELF-SERVICE
    // ═════════════════════════════════════════

    static void userIssueBook() {
        printTitle("ISSUE A BOOK");
        long active = activeIssuesForMember(currentUser.id);
        if (active >= MAX_ISSUES) {
            printError("You already have " + active + "/" + MAX_ISSUES + " books issued. Return a book first.");
            pause(); return;
        }
        double myFines = issuances.stream()
            .filter(i -> i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED"))
            .mapToDouble(Issuance::calculateFine).sum();
        if (myFines > 0) {
            printWarning("You have pending fines of Rs." + String.format("%.1f", myFines));
        }

        String bookId = input("Enter Book ID");
        Book book = findBook(bookId);
        if (book == null) { printError("Book not found."); pause(); return; }
        if (book.availableCopies <= 0) {
            printError("'" + book.title + "' is currently unavailable.");
            printInfo("You can reserve it instead (option 6).");
            pause(); return;
        }
        boolean already = issuances.stream().anyMatch(
            i -> i.bookId.equals(bookId) && i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED"));
        if (already) { printError("You already have this book issued."); pause(); return; }

        String dueDate = addDays(todayStr(), LOAN_DAYS);
        System.out.println("  Book      : " + book.title + " by " + book.author);
        System.out.println("  Issue Date: " + todayStr());
        System.out.println("  Due Date  : " + dueDate);
        System.out.println("  Fine Rate : Rs." + FINE_RATE + "/day if overdue");
        String confirm = input("Issue this book? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Issue cancelled."); pause(); return; }

        Issuance iss = new Issuance(bookId, currentUser.id, todayStr(), dueDate);
        issuances.add(iss);
        book.availableCopies--;
        book.issueCount++;

        logActivity(currentUser.name + " issued '" + book.title + "' (Issue ID: " + iss.id + ")");
        printSuccess("Book issued successfully!");
        System.out.println("  Issue ID  : " + iss.id);
        System.out.println("  Due Date  : " + dueDate);
        System.out.println("  You may be fined Rs." + FINE_RATE + "/day if returned late.");
        pause();
    }

    static void userReturnBook() {
        printTitle("RETURN A BOOK");
        List<Issuance> myActive = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED")) {
                i.refreshStatus(); myActive.add(i);
            }
        }
        if (myActive.isEmpty()) { printInfo("You have no active issues to return."); pause(); return; }

        System.out.printf("  %-8s %-32s %-12s %-10s%n","ISSUE ID","BOOK","DUE DATE","STATUS");
        System.out.println(THIN_LINE);
        myActive.forEach(i -> {
            Book b = findBook(i.bookId);
            System.out.printf("  %-8s %-32s %-12s %s%n",
                i.id, truncate(b != null ? b.title : i.bookId, 30), i.dueDate, i.status);
        });
        System.out.println(LINE);

        String issId = input("Enter Issue ID to return");
        Issuance iss = issuances.stream()
            .filter(i -> i.id.equalsIgnoreCase(issId) && i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED"))
            .findFirst().orElse(null);
        if (iss == null) { printError("Issue record not found or not yours."); pause(); return; }

        double fine = iss.calculateFine();
        Book book = findBook(iss.bookId);
        if (fine > 0) {
            System.out.printf("%n  ⚠ You have a fine of Rs.%.1f for this book.%n", fine);
        }

        String confirm = input("Confirm return? (yes/no)");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Return cancelled."); pause(); return; }

        iss.returnDate = todayStr();
        iss.status     = "RETURNED";
        iss.finePaid   = fine;
        if (book != null) book.availableCopies++;

        // Check for waiting reservations
        if (book != null) {
            reservations.stream()
                .filter(r -> r.bookId.equals(book.id) && r.status.equals("PENDING"))
                .findFirst()
                .ifPresent(r -> {
                    r.status = "AVAILABLE";
                    User ru = findUser(r.memberId);
                    printInfo("'" + book.title + "' is now available for "
                        + (ru != null ? ru.name : r.memberId) + "'s reservation.");
                });
        }

        if (fine > 0) {
            Fine fineRec = new Fine(iss.id, iss.memberId, fine);
            fineRec.status = "PAID"; fineRec.paidDate = todayStr();
            fines.add(fineRec);
        }

        logActivity(currentUser.name + " returned '" + (book != null ? book.title : iss.bookId) + "'"
            + (fine > 0 ? " | Fine: Rs." + String.format("%.1f", fine) : ""));
        printSuccess("Book returned successfully!");
        if (fine > 0) printInfo("Fine of Rs." + String.format("%.1f", fine) + " has been recorded.");
        pause();
    }

    static void viewMyBooks() {
        printTitle("MY ISSUED BOOKS");
        issuances.forEach(Issuance::refreshStatus);
        List<Issuance> mine = new ArrayList<>();
        for (Issuance i : issuances) {
            if (i.memberId.equals(currentUser.id) && !i.status.equals("RETURNED")) mine.add(i);
        }
        if (mine.isEmpty()) { printInfo("You have no active issues."); pause(); return; }

        double totalFine = 0;
        System.out.printf("  %-8s %-32s %-12s %-12s %-10s %s%n",
            "ISSUE ID","BOOK","ISSUE DATE","DUE DATE","STATUS","FINE");
        System.out.println(THIN_LINE);
        for (Issuance i : mine) {
            Book b = findBook(i.bookId);
            double fine = i.calculateFine();
            totalFine += fine;
            System.out.printf("  %-8s %-32s %-12s %-12s %-10s Rs.%.1f%n",
                i.id, truncate(b != null ? b.title : i.bookId, 30),
                i.issueDate, i.dueDate, i.status, fine);
        }
        System.out.println(THIN_LINE);
        System.out.printf("  Active Issues: %d/%d    Total Pending Fine: Rs.%.1f%n",
            mine.size(), MAX_ISSUES, totalFine);
        System.out.println(LINE);
        pause();
    }

    // ═════════════════════════════════════════
    //  PROFILE MODULE
    // ═════════════════════════════════════════

    static void viewProfile() {
        printTitle("MY PROFILE");
        User u = currentUser;
        long active   = activeIssuesForMember(u.id);
        long total    = issuances.stream().filter(i -> i.memberId.equals(u.id)).count();
        long pending  = reservations.stream().filter(r -> r.memberId.equals(u.id) && r.status.equals("PENDING")).count();
        double fines  = issuances.stream().filter(i -> i.memberId.equals(u.id) && !i.status.equals("RETURNED"))
                                 .mapToDouble(Issuance::calculateFine).sum();

        System.out.println(THIN_LINE);
        System.out.println("  Member ID     : " + u.id);
        System.out.println("  Full Name     : " + u.name);
        System.out.println("  Email         : " + u.email);
        System.out.println("  Phone         : " + (u.phone.isEmpty() ? "N/A" : u.phone));
        System.out.println("  Role          : " + u.role.toUpperCase());
        System.out.println("  Joined        : " + u.joinDate);
        System.out.println(THIN_LINE);
        System.out.println("  Active Issues : " + active + " / " + MAX_ISSUES);
        System.out.println("  Total Issues  : " + total);
        System.out.println("  Reservations  : " + pending + " pending");
        System.out.printf( "  Pending Fines : Rs.%.1f%n", fines);
        System.out.println(LINE);
        pause();
    }

    static void updateProfile() {
        printTitle("UPDATE PROFILE");
        System.out.println("  (Press ENTER to keep current value)");
        System.out.println(THIN_LINE);

        String name = input("New Name [" + currentUser.name + "]");
        if (!name.isEmpty()) currentUser.name = name;

        String phone = input("New Phone [" + currentUser.phone + "]");
        if (!phone.isEmpty()) currentUser.phone = phone;

        String oldPass = input("Current Password (to change password)");
        if (!oldPass.isEmpty()) {
            if (!oldPass.equals(currentUser.password)) { printError("Incorrect current password."); pause(); return; }
            String newPass = input("New Password (min 6 chars)");
            String confPass = input("Confirm New Password");
            if (newPass.length() < 6) { printError("Password too short."); pause(); return; }
            if (!newPass.equals(confPass)) { printError("Passwords don't match."); pause(); return; }
            currentUser.password = newPass;
            printSuccess("Password changed.");
        }

        // Sync with db
        User dbUser = findUser(currentUser.id);
        if (dbUser != null) {
            dbUser.name = currentUser.name;
            dbUser.phone = currentUser.phone;
            dbUser.password = currentUser.password;
        }

        logActivity(currentUser.name + " updated their profile.");
        printSuccess("Profile updated successfully.");
        pause();
    }

    // ═════════════════════════════════════════
    //  CONTACT LIBRARY
    // ═════════════════════════════════════════

    static void contactLibrary() {
        printTitle("CONTACT LIBRARY");
        System.out.println("  Library Email  : library@libranet.edu");
        System.out.println("  Phone          : +91 98765 43210");
        System.out.println("  Location       : 2nd Floor, Academic Block");
        System.out.println("  Working Hours  : Mon-Sat, 9 AM – 8 PM");
        System.out.println(THIN_LINE);
        System.out.println("  Quick Rules:");
        System.out.println("   • Maximum " + MAX_ISSUES   + " books per member at a time");
        System.out.println("   • Loan period : " + LOAN_DAYS   + " days");
        System.out.println("   • Fine rate   : Rs." + FINE_RATE + "/day overdue");
        System.out.println("   • Max reserves: " + MAX_RESERVES + " per member");
        System.out.println(THIN_LINE);
        System.out.println("  Send a Message:");
        String subject = input("Subject");
        String message = input("Message");
        if (!subject.isEmpty() && !message.isEmpty()) {
            logActivity(currentUser.name + " sent a query: " + subject);
            printSuccess("Message sent! We'll respond within 24 hours.");
        }
        pause();
    }

    // ═════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ═════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println(LINE);
        System.out.println("  Initializing LibraNet Library Management System...");
        seedData();
        System.out.println("  System ready. " + books.size() + " books and " + users.size() + " users loaded.");
        System.out.println(LINE);

        try {
            authMenu();
        } catch (Exception e) {
            System.out.println("\n  Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sc != null) sc.close();
        }
    }
}
