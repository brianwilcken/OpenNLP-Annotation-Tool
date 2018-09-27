package common;

import java.io.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.springframework.core.io.ClassPathResource;

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
            fileString = Files.toString(resource.getFile(), Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
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
}
