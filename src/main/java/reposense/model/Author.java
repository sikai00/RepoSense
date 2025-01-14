package reposense.model;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Git Author.
 */
public class Author {
    public static final String NAME_NO_AUTHOR_WITH_COMMITS_FOUND =
            "NO AUTHOR WITH COMMITS FOUND WITHIN THIS PERIOD OF TIME";
    private static final String UNKNOWN_AUTHOR_GIT_ID = "-";

    private static final String STANDARD_GITHUB_EMAIL_DOMAIN = "@users.noreply.github.com";
    private static final String STANDARD_GITLAB_EMAIL_DOMAIN = "@users.noreply.gitlab.com";

    private static final String MESSAGE_UNCOMMON_EMAIL_PATTERN = "The provided email, %s, uses uncommon pattern.";
    private static final String MESSAGE_UNCOMMON_GLOB_PATTERN = "The provided ignore glob, %s, uses uncommon pattern.";
    private static final String COMMON_EMAIL_REGEX =
            "^([a-zA-Z0-9_\\-\\.\\+]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
    private static final String COMMON_GLOB_REGEX = "^[-a-zA-Z0-9 _/\\\\*!{}\\[\\]!(),:.]*$";

    public static final Author UNKNOWN_AUTHOR = new Author(UNKNOWN_AUTHOR_GIT_ID);

    private final String gitId;

    private transient List<String> emails;
    private transient String displayName;
    private transient List<String> authorAliases;
    private transient List<String> ignoreGlobList;
    private transient PathMatcher ignoreGlobMatcher;

    public Author(String gitId) {
        this.gitId = gitId;
        this.emails = new ArrayList<>();
        this.displayName = gitId;
        this.authorAliases = new ArrayList<>();
        this.ignoreGlobList = new ArrayList<>();

        addStandardGitHostEmails(this.emails);
        updateIgnoreGlobMatcher();
    }

    public Author(StandaloneAuthor sa) {
        String gitId = sa.getGitId();
        List<String> emails = new ArrayList<>(sa.getEmails());
        String displayName = !sa.getDisplayName().isEmpty() ? sa.getDisplayName() : sa.getGitId();
        List<String> authorAliases = sa.getAuthorNames();
        List<String> ignoreGlobList = sa.getIgnoreGlobList();

        this.gitId = gitId;
        this.displayName = displayName;
        this.authorAliases = authorAliases;

        setEmails(emails);
        setIgnoreGlobList(ignoreGlobList);
    }

    public Author(Author another) {
        this.gitId = another.gitId;
        this.emails = another.emails;
        this.displayName = another.gitId;
        this.authorAliases = another.authorAliases;
        this.ignoreGlobList = another.ignoreGlobList;
        this.ignoreGlobMatcher = another.ignoreGlobMatcher;
    }

    /**
     * Checks that all the strings in the {@code emails} only contains commonly used email patterns.
     *
     * @throws IllegalArgumentException if any of the values do not meet the criteria.
     */
    private static void validateEmails(List<String> emails) throws IllegalArgumentException {
        for (String email : emails) {
            if (!email.matches(COMMON_EMAIL_REGEX)) {
                throw new IllegalArgumentException(String.format(MESSAGE_UNCOMMON_EMAIL_PATTERN, email));
            }
        }
    }

    /**
     * Checks that all the strings in the {@code ignoreGlobList} only contains commonly used glob patterns.
     *
     * @throws IllegalArgumentException if any of the values do not meet the criteria.
     */
    private static void validateIgnoreGlobs(List<String> ignoreGlobList) throws IllegalArgumentException {
        for (String glob : ignoreGlobList) {
            if (!glob.matches(COMMON_GLOB_REGEX)) {
                throw new IllegalArgumentException(String.format(MESSAGE_UNCOMMON_GLOB_PATTERN, glob));
            }
        }
    }

    public String getGitId() {
        return gitId;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        validateEmails(emails);
        this.emails = new ArrayList<>(emails);
        addStandardGitHostEmails(this.emails);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getAuthorAliases() {
        return authorAliases;
    }

    public void setAuthorAliases(List<String> authorAliases) {
        this.authorAliases = authorAliases;
    }

    public List<String> getIgnoreGlobList() {
        return ignoreGlobList;
    }

    public void setIgnoreGlobList(List<String> ignoreGlobList) {
        validateIgnoreGlobs(ignoreGlobList);
        this.ignoreGlobList = new ArrayList<>(ignoreGlobList);
        updateIgnoreGlobMatcher();
    }

    /**
     * Validates and adds {@code ignoreGlobList} into the {@link Author} class instance variable without duplicates
     * and updates the ignore glob matcher.
     */
    public void importIgnoreGlobList(List<String> ignoreGlobList) {
        validateIgnoreGlobs(ignoreGlobList);
        ignoreGlobList.forEach(ignoreGlob -> {
            if (!this.ignoreGlobList.contains(ignoreGlob)) {
                this.ignoreGlobList.add(ignoreGlob);
            }
        });
        updateIgnoreGlobMatcher();
    }

    /**
     * Returns true if this author is ignoring the {@code filePath} based on its ignore glob matcher.
     */
    public boolean isIgnoringFile(Path filePath) {
        return ignoreGlobMatcher.matches(filePath);
    }

    @Override
    public boolean equals(Object other) {
        // short circuit if same object
        if (this == other) {
            return true;
        }

        // instanceof handles null
        if (!(other instanceof Author)) {
            return false;
        }

        Author otherAuthor = (Author) other;
        return this.gitId.equalsIgnoreCase(otherAuthor.gitId);
    }

    @Override
    public int hashCode() {
        return gitId != null ? gitId.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return gitId;
    }

    /**
     * Updates the {@link PathMatcher} to the new ignore glob list set.
     * Called after a new ignore glob list is set.
     */
    private void updateIgnoreGlobMatcher() {
        String globString = "glob:{" + String.join(",", ignoreGlobList) + "}";
        ignoreGlobMatcher = FileSystems.getDefault().getPathMatcher(globString);
    }

    /**
     * Adds the standard github and gitlab emails to {@code emails} if not present.
     */
    private void addStandardGitHostEmails(List<String> emails) {
        String standardGitHubEmail = getGitId() + STANDARD_GITHUB_EMAIL_DOMAIN;
        String standardGitLabEmail = getGitId() + STANDARD_GITLAB_EMAIL_DOMAIN;
        if (!emails.contains(standardGitHubEmail)) {
            emails.add(standardGitHubEmail);
        }
        if (!emails.contains(standardGitLabEmail)) {
            emails.add(standardGitLabEmail);
        }
    }
}

