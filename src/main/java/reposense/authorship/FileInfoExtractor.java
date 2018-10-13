import reposense.git.CommitNotFoundException;
    private static final String FILE_DELETED_SYMBOL = "/dev/null";
    private static final Pattern FILE_CHANGED_PATTERN = Pattern.compile("\n(\\+){3} b?/(?<filePath>.*)\n");
        List<FileInfo> fileInfos = new ArrayList<>();

        try {
            GitChecker.checkoutToDate(config.getRepoRoot(), config.getBranch(), config.getUntilDate());
        } catch (CommitNotFoundException cnfe) {
            return fileInfos;
        }
            Matcher filePathMatcher = FILE_CHANGED_PATTERN.matcher(fileDiffResult);

            // diff result does not have the markers to indicate that file has any line changes, skip it
            if (!filePathMatcher.find()) {
            String filePath = filePathMatcher.group(FILE_CHANGED_GROUP_NAME);

            // file is deleted, skip it as well
            if (filePath.equals(FILE_DELETED_SYMBOL)) {
                continue;
            }