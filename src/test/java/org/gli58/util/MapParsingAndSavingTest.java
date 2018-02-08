package org.gli58.util;

import org.gli58.domain.City;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MapParsingAndSavingTest {

    @Rule
    public TemporaryFolder mapOutputFolder = new TemporaryFolder();

    @Test
    public void testParsingAndSavingWithSeveralCities() {
        List<String> lines = Arrays.asList(
            "Denalmo north=Agixo-A south=Amolusnisnu east=Elolesme west=Migina",
            "Asnu north=Ago-Mo south=Emexisno east=Dinexe west=Amiximine",
            "Esmosno north=Emexege south=Anegu east=Axesminilmo west=Dosmolixo",
            "Age north=Enusmu south=Amilolna east=Mo-Mulu west=Amagagiga",
            "Axagene north=Exala south=Deniniga east=Digeno west=Asmudu"
        );

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

            final String NEWLINE = System.getProperty("line.separator");

            //Note about the last NEWLINE
            String citiesRead = lines.stream().collect(Collectors.joining(NEWLINE, "", NEWLINE));

            StringWriter sw = new StringWriter();
            MapIO.writeCities(cities, new PrintWriter(sw));
            String citiesWritten = sw.toString();

            assertThat(citiesWritten).isEqualTo(citiesRead);
        }
    }

    @Test
    public void testParsingAndSavingTheEntireMap() throws IOException {

        String mapResourcePath = "map.txt";
        final Set<City> cities = MapIO.getCitiesFromClasspathResource(mapResourcePath);
        ClassLoader classLoader = this.getClass().getClassLoader();
        File inputFile = new File(classLoader.getResource(mapResourcePath).getFile());

        File outputFile = mapOutputFolder.newFile("map_saved.txt");
        MapIO.writeCitiesToFile(cities, outputFile);

        //first compare file size
        assertThat(inputFile.length()).isEqualTo(outputFile.length());

        //then compare file content
        Iterator<String> inputIter = Files.lines(inputFile.toPath()).iterator();
        Iterator<String> outputIter = Files.lines(outputFile.toPath()).iterator();

        while (inputIter.hasNext() && outputIter.hasNext()) {
            assertThat(inputIter.next()).isEqualTo(outputIter.next());
        }

        assertThat(inputIter.hasNext()).isFalse();
        assertThat(outputIter.hasNext()).isFalse();
    }
}
