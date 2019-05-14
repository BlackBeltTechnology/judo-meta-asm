package hu.blackbelt.judo.meta.asm.runtime;

import lombok.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@AllArgsConstructor
@ToString
@Getter
@Builder(builderMethodName = "asmModelBuilder")
public class AsmModel {

    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String CHECKSUM = "checksum";
    public static final String META_VERSION_RANGE = "meta-version-range";
    public static final String URI = "uri";
    public static final String RESOURCESET = "resourceset";

    @NonNull
    String name;
    @NonNull
    String version;
    @NonNull
    URI uri;
    String checksum;
    String metaVersionRange;
    @NonNull
    ResourceSet resourceSet;

    public Dictionary<String, Object> toDictionary() {
        Dictionary<String, Object> ret = new Hashtable<>();
        ret.put(NAME, name);
        ret.put(VERSION, version);
        ret.put(URI, uri);
        ret.put(CHECKSUM, checksum);
        ret.put(META_VERSION_RANGE, metaVersionRange);
        ret.put(RESOURCESET, resourceSet);

        return ret;
    }


    public <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    public <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public <T> Stream<T> all() {
        return asStream((Iterator<T>) resourceSet.getAllContents(), false);
    }

}
