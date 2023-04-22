public class File {
    private String name;
    private String checksum;

    public File(String name, String checksum) {
        this.name = name;
        this.checksum = checksum;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
