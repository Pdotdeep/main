import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import movieRequesterAPI.RequestListener;
import movieRequesterAPI.RetrieveRequest;
import object.MovieInfoObject;
import ui.Ui;

import java.io.IOException;
import java.util.ArrayList;

public class MovieHandler extends Controller implements RequestListener{
    @FXML
    private ScrollPane mMoviesScrollPane;

    @FXML
    private VBox mMovieTypeVBox;

    @FXML
    private Label titleLabel;

    @FXML
    private Label titleLabel2;

    @FXML
    private Text ageText;

    @FXML
    private Text genreText;

    @FXML
    private Text text;


    private FlowPane mMoviesFlowPane;

    //@FXML
    //private ListView<String> mMovieTypeListView;

    @FXML
    private Label mStatusLabel;

    @FXML
    private ProgressBar mProgressBar;

    @FXML
    private TextField mSearchTextField;

    //@FXML
    //private Button mSearchButton;

    //@FXML
    //private Button mClearSearchButton;

    private ArrayList<MovieInfoObject> mMovies;
    private double[] mImagesLoadingProgress;
    private RetrieveRequest mMovieRequest;


    @FXML public void initialize()
    {
        mMovieRequest = new RetrieveRequest(this);

        //mMovieTypeVBox.setStyle("-fx-border-color: white;");





        //mMovieTypeListView.getItems().addAll("Now Showing", "Popular", "TV Shows", "Upcoming Movies");
        //mMovieTypeListView.getSelectionModel().select(0);


        mSearchTextField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                System.out.println("Tab pressed");
                event.consume();
            }else if(event.getCode().equals(KeyCode.ENTER)) {
                System.out.println("Enter pressed");
//
            }
        });

        mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.NOW_SHOWING);


        //mSearchButton.disableProperty().bind(mSearchTextField.textProperty().isEmpty());
        //mClearSearchButton.disableProperty().bind(mSearchTextField.textProperty().isEmpty());


       // mMovieTypeListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> moviesTypeSelectionChanged(oldValue.intValue(), newValue.intValue()));
    }

    // Called when the fetch request for the movie data is completed
    @Override
    public void requestCompleted(ArrayList<MovieInfoObject> moviesInfo)
    {
        // Build the Movie poster views and add to the flow pane on the main thread
        mMovies = moviesInfo;
        mImagesLoadingProgress = new double[mMovies.size()];
        Platform.runLater(() -> buildMoviesFlowPane(moviesInfo));
    }

    // Called when the request to fetch movie data timed out.
    @Override
    public void requestTimedOut()
    {
        Platform.runLater(() -> showDownloadFailureAlert("Request timed out"));
    }

    // Called when the request fails due to an invalid internet connection
    @Override
    public void requestFailed()
    {
        Platform.runLater(() -> showDownloadFailureAlert("No internet connection"));
    }


    private void showDownloadFailureAlert(String headerText)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to download");
        alert.setHeaderText(headerText);
        alert.setContentText("Please ensure you have an active internet connection.");
        alert.showAndWait();
    }

    private void buildMoviesFlowPane(ArrayList<MovieInfoObject> movies)
    {
        // Setup progress bar and status label
        mProgressBar.setProgress(0.0);
        mProgressBar.setVisible(true);
        mStatusLabel.setText("Loading..");

        // Build a flow pane layout with the width and size of the
        mMoviesFlowPane = new FlowPane(Orientation.HORIZONTAL);
        mMoviesFlowPane.setHgap(4);
        mMoviesFlowPane.setVgap(10);
        mMoviesFlowPane.setPadding(new Insets(10, 8, 4, 8));
        mMoviesFlowPane.prefWrapLengthProperty().bind(mMoviesScrollPane.widthProperty());   // bind to scroll pane width

        for (MovieInfoObject movie : movies)
        {
            AnchorPane posterPane = buildMoviePosterPane(movie);
            mMoviesFlowPane.getChildren().add(posterPane);
        }

        mMoviesScrollPane.setContent(mMoviesFlowPane);
    }


    private AnchorPane buildMoviePosterPane(MovieInfoObject movie) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MovieHandler.class.getResource("MoviePoster.fxml"));
            AnchorPane posterView = loader.load();
            posterView.setOnMouseClicked((mouseEvent) -> moviePosterClicked(movie));

            // set the movie info
            MoviePosterController controller = loader.getController();
            Image posterImage = new Image(movie.getFullPosterPath(), true);
            posterImage.progressProperty().addListener((observable, oldValue, newValue) -> updateProgressBar(movie, newValue.doubleValue()));

            controller.getMovieTitleLabel().setText(movie.getTitle());
            controller.getPosterImageView().setImage(posterImage);

            return posterView;
        } catch (IOException ex){
            Ui.printLine();
        }

        return null;
    }

    private void updateProgressBar(MovieInfoObject movie, double progress)
    {
        // update the progress for that movie in the array
        int index = mMovies.indexOf(movie);
        if (index >= 0){
            mImagesLoadingProgress[index] = progress;
        }

        double currentTotalProgress = 0.0;
        for (double value : mImagesLoadingProgress){
            currentTotalProgress += value;
        }

        //System.out.println("Current total progress: " + currentTotalProgress);
        mProgressBar.setProgress((currentTotalProgress / mMovies.size()));

        if(currentTotalProgress >= mMovies.size()){
            mProgressBar.setVisible(false);
            mStatusLabel.setText("");
        }
    }

    // movieTypeSelectionChanged event - Reload the movies for the new movies request type
    private void moviesTypeSelectionChanged(int oldIndex, int newIndex)
    {
        if(newIndex >= 0 && (newIndex != oldIndex)){

            if (mMoviesFlowPane != null) {
                mMoviesFlowPane.getChildren().clear();
            }


            switch (newIndex) {
                case 0:
                    mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.NOW_SHOWING);
                    break;

                case 1:
                    mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.POPULAR);
                    break;

                case 2:
                    mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.TV_SHOWS);
                    break;

                case 3:
                    mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.UPCOMING);
                    break;

                default:
                    break;
            }
        }
    }

    // User clicks on a movie poster
    private void moviePosterClicked(MovieInfoObject movie) {

        mMainApplication.transitToMovieInfoController(movie);
    }

    @FXML private void searchButtonClicked() {
        if (mSearchTextField.getText().equals("show current movie")) {
            mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.NOW_SHOWING);
        } else if (mSearchTextField.getText().equals("show upcoming movie")) {
            mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.UPCOMING);
        } else if (mSearchTextField.getText().equals("show popular movie")) {
            mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.POPULAR);
        } else if (mSearchTextField.getText().equals("show current tv")) {
            mMovieRequest.beginMovieRequest(RetrieveRequest.MoviesRequestType.TV_SHOWS);
        } else if (!mSearchTextField.getText().isEmpty()) {
            mMovieRequest.beginSearchRequest(mSearchTextField.getText());
        }
    }

    @FXML private void clearSearchButtonClicked()
    {
        mSearchTextField.clear();
    }

    // Menu item events
    @FXML public void exitMenuItemClicked()
    {
        System.exit(0);
    }

    @FXML public void aboutMenuItemClicked()
    {
    }
}