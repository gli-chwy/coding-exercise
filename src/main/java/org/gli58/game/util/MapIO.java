package org.gli58.game.util;

import org.gli58.game.exceptions.MapParsingException;
import org.gli58.game.exceptions.MapSavingToFileException;
import org.gli58.game.City;
import org.gli58.game.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * Utility for parsing map file into map data, as well as writing map data to external file.
 *
 * It's intendeded to be used through its utility (public static) APIs, and cannot be
 * instantiated by the client.
 *
 * The line format it read and writes is:
 * Denalmo north=Agixo-A south=Amolusnisnu east=Elolesme west=Migina
 * Asnu north=Ago-Mo south=Emexisno east=Dinexe west=Amiximine
 * ...
 *
 * Explanation of the format is in the instructions.md document.
 *
 */
public class MapIO {
    private static Logger logger = LoggerFactory.getLogger(MapIO.class);

    private static final String FIELD_SEPARATOR = " ";

    private static final String NORTH_PREFIX = "north=";
    private static final String SOUTH_PREFIX = "south=";
    private static final String EAST_PREFIX = "east=";
    private static final String WEST_PREFIX = "west=";

    private static final int NORTH_PREFIX_LEN = NORTH_PREFIX.length();
    private static final int SOUTH_PREFIX_LEN = SOUTH_PREFIX.length();
    private static final int EAST_PREFIX_LEN = EAST_PREFIX.length();
    private static final int WEST_PREFIX_LEN = WEST_PREFIX.length();

    private static final String NEWLINE = System.getProperty("line.separator");

    private static Map<Direction, String> encodedDirections = new EnumMap<Direction, String>(Direction.class);

    static {
        encodedDirections.put(Direction.NORTH, NORTH_PREFIX);
        encodedDirections.put(Direction.SOUTH, SOUTH_PREFIX);
        encodedDirections.put(Direction.EAST, EAST_PREFIX);
        encodedDirections.put(Direction.WEST, WEST_PREFIX);
    }

    private MapIO() {} //prevents direct instantiation by client

    public static Set<City> getCitiesFromClasspathResource(String resourcePath) {
        ClassLoader classLoader = MapIO.class.getClassLoader();

        try {
            URI resourceUri = classLoader.getResource(resourcePath).toURI();

            return getCitiesFromStream(Files.lines(Paths.get(resourceUri)));

        } catch (URISyntaxException e) {
            throw new MapParsingException("failed to parse " + resourcePath, e);

        } catch (IOException e) {
            throw new MapParsingException("failed to parse " + resourcePath, e);
        }
    }

    public static Set<City> getCitiesFromStream(Stream<String> lines) {
        return fromStreamOfCities(lines);
    }

    private static Set<City> fromStreamOfCities(Stream<String> lines) {
        //Note explicitly using LinkedHashSet to maintain the order of cities parsed -
        //not essential but somewhat nice

        Map<String, City> cities = new LinkedHashMap<>();
        return lines.map(line -> parseOneCity(cities, line)).collect(toCollection(LinkedHashSet::new));
    }

    private static City parseOneCity(Map<String, City> cities, String line) {
        String[] parts = line.split(FIELD_SEPARATOR);

        if (parts == null || parts.length < 2) { //2 here assuming city name plus at least one neighbor
            throw new IllegalArgumentException(
                    "a city needs to have at least one neighbor " + line);
        }
        City fromCity = cities.computeIfAbsent(parts[0], City::new);

        for (int i=1; i<parts.length; i++) {
            String neighbor = parts[i];
            switch (neighbor.charAt(0)) {
                case 'n':
                    City north = cities.computeIfAbsent(neighbor.substring(NORTH_PREFIX_LEN), City::new);
                    fromCity.addNeighbor(Direction.NORTH, north);
                    break;
                case 'e':
                    City east = cities.computeIfAbsent(neighbor.substring(EAST_PREFIX_LEN), City::new);
                    fromCity.addNeighbor(Direction.EAST, east);
                    break;
                case 's':
                    City south = cities.computeIfAbsent(neighbor.substring(SOUTH_PREFIX_LEN), City::new);
                    fromCity.addNeighbor(Direction.SOUTH, south);
                    break;
                case 'w':
                    City west = cities.computeIfAbsent(neighbor.substring(WEST_PREFIX_LEN), City::new);
                    fromCity.addNeighbor(Direction.WEST, west);
                    break;

                default:
                    throw new IllegalArgumentException("unknown neighbor type " + neighbor);
            }
        }

        return fromCity;
    }

    public static void writeCitiesToFile(Set<City> cities, String fileName) {
        writeCitiesToFile(cities, new File(fileName));
    }

    public static void writeCitiesToFile(Set<City> cities, File file) {
        try (PrintWriter writer = new PrintWriter(file, "UTF-8"))
        {
            writeCities(cities, writer);

        } catch (FileNotFoundException e) {
            throw new MapSavingToFileException("cannot save cities to file " + file.getAbsolutePath(), e);

        } catch (UnsupportedEncodingException e) {
            throw new MapSavingToFileException("cannot save cities to file", e);
        }
    }

    //Note client is responsible for closing the output writer stream
    public static void writeCities(Set<City> cities, PrintWriter writer) {
        for (City city : cities) {
            writer.print(city.getName());
            if (city.getNeighbors().size() > 0) {
                writer.print(FIELD_SEPARATOR);
                String nbrText = city.getNeighbors().entrySet().stream()
                        .map(e -> encodedDirections.get(e.getKey()) + e.getValue().getName())
                        .collect(joining(FIELD_SEPARATOR));
                writer.print(nbrText);
            }
            writer.println();
        }

        writer.flush();
    }

    public static String writeCitiesAsString(Set<City> cities) {
        StringWriter sw = new StringWriter();
        MapIO.writeCities(cities, new PrintWriter(sw));
        return sw.toString();
    }
}
