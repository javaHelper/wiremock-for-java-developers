package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class MoviesRestClientTest {
    private MoviesRestClient moviesRestClient;
    private WebClient webClient;

    @BeforeEach
    public void setup() {
        int port = 8081;
        final String baseUrl = String.format("http://localhost:%s/", port);

        webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    @DisplayName("All Movies")
    void getAllMovies() {
        assertThat(moviesRestClient.retrieveAllMovies())
                .isNotNull()
                .hasSizeGreaterThan(5);
    }

    @DisplayName("Movie by Id")
    @Test
    void retrieveMovieById() {
        Movie movie = moviesRestClient.retrieveMovieById(1);
        assertThat(movie)
                .isNotNull()
                .extracting(Movie::getMovie_id, Movie::getName, Movie::getYear)
                .containsExactly(1L, "Batman Begins", 2005);
    }


    @Test
    @DisplayName("Movie by Id - Not Found")
    void retrieveMovieById_NotFound() {
        Integer movieId = 100;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(movieId));
    }

    @DisplayName("Movie by Name")
    @Test
    void retrieveMovieByName() {
        String movieName = "Avengers";
        List<Movie> movies = moviesRestClient.retrieveMovieByName(movieName);

        assertThat(movies)
                .isNotNull()
                .hasSize(4)
                .extracting(Movie::getName)
                .containsExactly("The Avengers",
                        "Avengers: Age of Ultron",
                        "Avengers: Infinity War",
                        "Avengers: End Game");
    }

    @Test
    void retrieveMovieByName_Not_Found() {
        String movieName = "ABC";
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByName(movieName));
    }

    @Test
    @DisplayName("Movie by Year")
    void retrieveMovieByYear() {
        Integer year = 2012;
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(year);

        assertThat(movieList)
                .isNotNull()
                .hasSize(2)
                .extracting(Movie::getName)
                .containsExactly("The Dark Knight Rises", "The Avengers");
    }

    @Test
    void retrieveMovieByYear_Not_Found() {
        Integer year = 1950;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByYear(year));
    }

    @Test
    @DisplayName("Add New Movie")
    void addNewMovie() {
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null,
                "Toy Story 4",
                2019,
                batmanBeginsCrew,
                LocalDate.of(2019, 06, 20));

        Movie movie = moviesRestClient.addNewMovie(toyStory);

        assertThat(movie)
                .isNotNull()
                .extracting(Movie::getName, Movie::getYear)
                .containsExactly( "Toy Story 4", 2019);
    }

    @Test
    @DisplayName("Passing the Movie name and year as Null")
    void addNewMovie_InvlaidInput() {
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, null, null, batmanBeginsCrew, LocalDate.of(2019, 06, 20));

        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addNewMovie(toyStory));
    }

    @Test
    void updateMovie() {
        String darkNightRisesCrew = "Tom Hardy";
        Movie darkNightRises = new Movie(null, null, null, darkNightRisesCrew, null);
        Integer movieId = 3;

        Movie updatedMovie = moviesRestClient.updateMovie(movieId, darkNightRises);
        String updatedCastName = "Christian Bale, Heath Ledger , Michael Caine, Tom Hardy";
        Assertions.assertTrue(updatedMovie.getCast().contains(darkNightRisesCrew));
    }

    @Test
    void updateMovie_Not_Found() {
        String darkNightRisesCrew = "Tom Hardy";
        Movie darkNightRises = new Movie(null, null, null, darkNightRisesCrew, null);
        Integer movieId = 100;

        Assertions.assertThrows(MovieErrorResponse.class,()-> moviesRestClient.updateMovie(movieId, darkNightRises));
    }

    @Test
    void deleteMovie() {
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 4", 2019, batmanBeginsCrew, LocalDate.of(2019, 06, 20));
        Movie movie = moviesRestClient.addNewMovie(toyStory);
        Integer movieId=movie.getMovie_id().intValue();

        String response = moviesRestClient.deleteMovieById(movieId);
        String expectedResponse = "Movie Deleted Successfully";
        assertEquals(expectedResponse, response);
    }

    @Test
    void deleteMovie_notFound() {
        Integer movieId=100;
        Assertions.assertThrows(MovieErrorResponse.class, ()-> moviesRestClient.deleteMovieById(movieId)) ;
    }
}
