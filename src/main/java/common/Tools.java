package common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.core.io.ClassPathResource;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Tools {

    private static Properties _properties;
    private static List<Exception> _exceptions;

    public static Properties getProperties() {
        if (_properties == null) {
            _properties = new Properties();
            try {
                _properties.load(Tools.class.getClassLoader().getResourceAsStream("app.properties"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return _properties;
    }

    public static List<Exception> getExceptions() {
        if (_exceptions == null) {
            _exceptions = new ArrayList<>();
        }
        return _exceptions;
    }

    public static String getProperty(String property) {
        return getProperties().getProperty(property);
    }

    public static String getResource(String name) {
        ClassPathResource resource = new ClassPathResource(name);
        String fileString = null;
        try {
            fileString = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        return fileString;
    }

    public static String GetFileString(String filePath) {
        String fileString = null;
        try {
            fileString = Files.toString(new File(filePath), Charsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return fileString;
    }

    public static String GetFileString(String filePath, String encoding) {
        String fileString = null;
        try {
            return FileUtils.readFileToString(new File(filePath), encoding);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return fileString;
    }

    public static File WriteFileToDisk(String path, InputStream stream) throws IOException {
        File file = new File(path);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        org.apache.commons.io.IOUtils.copy(stream, fileOutputStream);
        stream.close();
        fileOutputStream.close();
        return file;
    }

    public static String GetQueryString(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> params = mapper.convertValue(obj,  Map.class);

        String queryString = params.entrySet().stream().map(p -> p.getKey().toString() + "=" + p.getValue().toString())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .map(s -> "?" + s)
                .orElse("");

        return queryString;
    }

    public static String getFormattedDateTimeString(Instant dateTime) {
        return dateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }

    public static int[] argsort(final double[] a) {
        return argsort(a, true);
    }

    public static int[] argsort(final double[] a, final boolean ascending) {
        Integer[] indexes = new Integer[a.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return (ascending ? 1 : -1) * Double.compare(a[i1], a[i2]);
            }
        });
        return asArray(indexes);
    }

    public static <T extends Number> int[] asArray(final T... a) {
        int[] b = new int[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i].intValue();
        }
        return b;
    }

    public static final String UTF8_BOM = "\uFEFF";

    public static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    @FunctionalInterface
    public interface CheckedConsumer<T> {
        void apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface CheckedBiConsumer<T, U> {
        void apply(T t, U u) throws Exception;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static Comparator<Map<String, Object>> documentComparator = new Comparator<Map<String, Object>>() {
        public int compare(Map<String, Object> m1, Map<String, Object> m2) {
            return m1.get("filename").toString().compareTo(m2.get("filename").toString());
        }
    };

    public static <T> T loadXML(String xmlPath, Class<T> type) {
        try {
            ClassPathResource resource = new ClassPathResource(xmlPath);
            JAXBContext jaxbContext = JAXBContext.newInstance(type);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            T obj = (T) unmarshaller.unmarshal(resource.getInputStream());
            return obj;
        } catch (JAXBException | IOException e) {
            return null;
        }
    }

    public static List<String> extractEntriesFromDictionary(dictionary.Dictionary dict) {
        List<String> entries = dict.getEntry().stream().map(p -> StringUtils.join(p.getToken(), " ")).collect(Collectors.toList());

        return entries;
    }

    public static double similarity(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 1.0;
        } else if ((s1 != null && s2 == null) || (s1 == null && s2 != null)) {
            return 0.0;
        } else {
            String longer = s1, shorter = s2;
            if (s1.length() < s2.length()) { // longer should always have greater length
                longer = s2; shorter = s1;
            }
            int longerLength = longer.length();
            if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
            return (longerLength - levenshteinDistance.apply(longer, shorter)) / (double) longerLength;
        }
    }
}
