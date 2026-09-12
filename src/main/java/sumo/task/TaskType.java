package sumo.task;

/** Represents the finite set of task types supported by Sumo. */
public enum TaskType {
    TODO("T", 3),
    DEADLINE("D", 4),
    EVENT("E", 5);

    private final String storageCode;
    private final int storedFieldCount;

    TaskType(String storageCode, int storedFieldCount) {
        this.storageCode = storageCode;
        this.storedFieldCount = storedFieldCount;
    }

    /** Returns the single-character code used in the task data file. */
    public String getStorageCode() {
        return storageCode;
    }

    /** Returns the number of fields required for this task type in storage. */
    public int getStoredFieldCount() {
        return storedFieldCount;
    }

    /**
     * Returns the task type represented by a stored type code.
     *
     * @param storageCode code read from the task data file
     * @return matching task type
     * @throws IllegalArgumentException if the code is not recognised
     */
    public static TaskType fromStorageCode(String storageCode) {
        for (TaskType taskType : values()) {
            if (taskType.storageCode.equals(storageCode)) {
                return taskType;
            }
        }
        throw new IllegalArgumentException("Unknown task type in data file: " + storageCode);
    }
}
