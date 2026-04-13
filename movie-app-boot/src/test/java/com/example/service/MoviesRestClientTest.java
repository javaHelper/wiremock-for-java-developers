package com.example.service;

import com.example.dto.Movie;
import com.example.exception.MovieErrorResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static com.example.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class MoviesRestClientTest {

    @Autowired
    MoviesRestClient moviesRestClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .globalTemplating(true)
                    .notifier(new ConsoleNotifier(true)))
            .build();

    @DynamicPropertySource
    static void overrideProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("movieapp.baseUrl",
                () -> "http://localhost:" + wireMockServer.getPort());
    }

    @BeforeEach
    void setup() {
        WireMock.configureFor("localhost", wireMockServer.getPort());
        WireMock.reset();
    }

    @Test
    void getAllMovies() {
        stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value()).withBodyFile("all-movies.json")));
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
        assertFalse(movieList.isEmpty());
    }

    @Test
    void retrieveMovieById() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        Movie movie = moviesRestClient.retrieveMovieById(1);
        assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieById_NotFound() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
                        .withBodyFile("404-movieId.json")));
        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(100));
    }

    @Test
    void retrieveMovieByName() {
        String movieName = "Avengers";
        stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo(movieName))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        assertEquals(4, movieList.size());
    }

    @Test
    void addNewMovie() {
        Movie toyStory = new Movie(null,
                "Toy Story 4",
                2019,
                "Tom Hanks, Tim Allen",
                LocalDate.of(2019, 6, 20));
        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name"))
                .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        Movie movie = moviesRestClient.addNewMovie(toyStory);
        assertNotNull(movie.getMovie_id());
    }

    @Test
    void deleteMovie_notFound() {
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovieById(100));
    }
}