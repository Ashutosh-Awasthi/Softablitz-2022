package com.example.sudoku;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

public class SudokuController   {

    public Label timeLabel;
    public Label consoleLabel;
    public Label nameLabel;
    public Button undoButton;
    public Button redoButton;
    public Button hintButton;
    protected Parent root;
    protected Stage stage;
    protected Game game;
    protected TextField[][] tfs;
    private GameStack gameStack;

    private boolean stopTimer = false;

    public void populate(Game game, Stage stage, Parent root, TextField[][] tfs)  {

        gameStack = new GameStack();
        this.tfs = tfs;
        this.game=  game;
        this.root = root;
        this.stage = stage;
        nameLabel.setText(game.getName());

        for(int i=0; i<game.getSize(); i++)
            for(int j=0; j<game.getSize(); j++) {
                if(game.grid[i][j] != 0){
                    tfs[i][j].setText(game.grid[i][j] + "");
                    tfs[i][j].setEditable(false);
                }else{
                    // code for eventListener for sudoku board text-fields
                    int finalI = i;
                    int finalJ = j;

                    tfs[i][j].addEventHandler(KeyEvent.KEY_RELEASED,keyEvent-> {
                            this.onKeyReleasedHandler(keyEvent, finalI, finalJ);
                    });

                    tfs[i][j].textProperty().addListener((observable, oldValue, newValue)->{
                        System.out.println("Triggered TP listener");
                        try {
                            int value = Integer.parseInt(newValue);
                            if(value==0)
                                throw new Exception();

                            if(value > game.getSize())
                                tfs[finalI][finalJ].setText(value % (game.getSize() + 1) +"");

                        } catch (Exception e){
                            System.out.println("Not a number");
                            tfs[finalI][finalJ].setText("");
                            game.blanks++;
                        }

                   });
                }
            }


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!game.isCompleted && !stopTimer){
                    Platform.runLater(()->{
                        timeLabel.setText("Time: " + game.time.getTime());
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    game.time.tick();
                }
            }
        }).start();

        stage.setOnCloseRequest(windowEvent -> {
            stopTimer = true;
        });

        if(gameStack.undoStack.isEmpty()){
            undoButton.setDisable(true);
        }
        if(gameStack.redoStack.isEmpty()){
            redoButton.setDisable(true);
        }
        if(game.getNumberOfHints()==0)
            hintButton.setDisable(true);
    }

    public void onSaveButtonClick(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save");
        fc.setInitialFileName("Savegame_"+new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("save file", "*.txt"));


        try{
             File file = fc.showSaveDialog(stage);
             System.out.println(file.getAbsoluteFile());

             ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.getAbsoluteFile()));
             oos.writeObject(game);
             oos.close();
             consoleLabel.setText("Game saved successfully");

        }catch (Exception e){
            e.printStackTrace();
            consoleLabel.setText("Game failed to save");
        }
    }

    public void onKeyReleasedHandler(KeyEvent keyEvent, int i, int j) {
        if(!keyEvent.getCode().isDigitKey())
            return;

        int value;
        if(tfs[i][j].getText().equals("")) {
            value = 0;
        }else{
            value = Integer.parseInt(tfs[i][j].getText());
        }

        // pushing value of previous state in stack
        gameStack.undoStack.push(new Game(game));
        game.grid[i][j] = value;

        // stack operations
        if(!gameStack.undoStack.isEmpty())
            undoButton.setDisable(false);

        if(!gameStack.redoStack.isEmpty()){
            gameStack.redoStack = new Stack<>();
            redoButton.setDisable(true);
        }

        // finish game code
        if(game.checkCompletedGame()){
            consoleLabel.setText("Game finished Successfully");
            root.setStyle("-fx-background-color: #ff950e;");
            onGameCompleteHandler();
        }else if(Checker.isValidMove(game.grid, i, j, game.getSize())){
            consoleLabel.setText("Valid Move");
            root.setStyle("-fx-background-color: #78f400;");

        }else{
            consoleLabel.setText("Invalid Move");
            root.setStyle("-fx-background-color: #f70000;");
        }
    }


    public void onUndoButtonClick(ActionEvent event) {
        gameStack.redoStack.push(new Game(game));
        redoButton.setDisable(false);
        game = gameStack.undoStack.pop();

        if(gameStack.undoStack.isEmpty()){
            undoButton.setDisable(true);
        }

        for(int i=0; i< game.getSize(); i++){
            for (int j=0; j< game.getSize(); j++){
                if(game.grid[i][j] != 0)
                    tfs[i][j].setText(game.grid[i][j] + "");
                else
                    tfs[i][j].setText("");
            }
        }
    }

    public void onRedoButtonClick(ActionEvent event) {
        gameStack.undoStack.push(new Game(game));
        undoButton.setDisable(false);
        game = gameStack.redoStack.pop();

        if(gameStack.redoStack.isEmpty()){
            redoButton.setDisable(true);
        }

        for(int i=0; i< game.getSize(); i++){
            for (int j=0; j< game.getSize(); j++){
                if(game.grid[i][j] != 0)
                    tfs[i][j].setText(game.grid[i][j] + "");
                else
                    tfs[i][j].setText("");
            }
        }
    }

    public void onGameCompleteHandler() {
        if(User.getInstance().getUsername()==null){
            return;
        }

        // use database
        try{
            Connection conn = DriverManager.getConnection(Settings.getInstance().getDB_URI(), Settings.getInstance().getDB_USERNAME(), Settings.getInstance().getDB_PASSWORD());
            Statement statement = conn.createStatement();

            String query = "INSERT INTO GAMEDATA (username, game_time, mode, board, uid) VALUES ( '" + game.getName() + "' , " + game.time.getTimeInSeconds() + " , '" + game.difficulty + "' , '"+ game.getSsize() + "' , '"+game.getId() + "' )";
            statement.executeUpdate(query);
            consoleLabel.setText("Game result Saved");
        }catch (SQLException e){
            consoleLabel.setText("Problem occurred saving the result");
            e.printStackTrace();
        }
    }

    public void onHintButtonClick(ActionEvent event) {

        ArrayList<Pair<Integer, Integer>> emptyboxes = new ArrayList<>();

        for(int i=0; i< game.getSize(); i++)
        {
            for(int j=0; j< game.getSize(); j++)
            {
                if(game.grid[i][j]==0){
                    Pair<Integer,Integer> p = new Pair<>(i,j);
                    emptyboxes.add(p);
                }
            }
        }

        int N = emptyboxes.size();
        int randidx = (int)(N*Math.random());

        int hi = emptyboxes.get(randidx).getKey();
        int hj = emptyboxes.get(randidx).getValue();

        gameStack.undoStack.push(new Game(game));
        game.grid[hi][hj] = game.solutionGrid[hi][hj];
        game.time.tick((int) Math.pow(1.3, -(game.getNumberOfHints()- 23)) + 15);

        if(!gameStack.undoStack.isEmpty())
            undoButton.setDisable(false);

        tfs[hi][hj].setText(game.solutionGrid[hi][hj] + "");
        timeLabel.setText("Time: "+game.time.getTime());

        game.numberOfHints--;
        if(game.numberOfHints==0)
            hintButton.setDisable(true);
    }

    public void onExitButtonClick(ActionEvent event) throws IOException {
        stopTimer = true;
        stage.close();
        FXMLLoader fxmlLoader = new FXMLLoader(StartPageController.class.getResource("StartPage.fxml"));
        Stage stage = new Stage();

        stage.setTitle("Sudoku");
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.getIcons().add(new Image(Objects.requireNonNull(Init.class.getResourceAsStream("mainico.png"))));
        stage.setResizable(false);
        stage.show();
    }
}
