import java.util.*;

public class SymbolTable {
    private Map<String, Entry> entries;

    public SymbolTable() {
        this.entries = new HashMap<>();
    }

    public void addEntry(String unid, String kind, String name, String scope) {
        entries.put(unid, new Entry(unid, kind, name, scope));
    }

    public Entry getEntry(String unid) {
        return entries.get(unid);
    }

    public Collection<Entry> getEntries() {
        return entries.values();
    }

    public boolean variableExists(String name, String scope) {
        for (Entry entry : entries.values()) {
            if (entry.getKind().equals("VARIABLE") && entry.getName().equals(name) && entry.getScope().equals(scope)) {
                return true;
            }
        }
        return false;
    }

    public boolean functionExists(String name) {
        for (Entry entry : entries.values()) {
            if (entry.getKind().equals("FUNCTION") && entry.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static class Entry {
        private String unid;
        private String kind;
        private String name;
        private String scope;

        public Entry(String unid, String kind, String name, String scope) {
            this.unid = unid;
            this.kind = kind;
            this.name = name;
            this.scope = scope;
        }

        public String getUnid() {
            return unid;
        }

        public String getKind() {
            return kind;
        }

        public String getName() {
            return name;
        }

        public String getScope() {
            return scope;
        }
    }
}