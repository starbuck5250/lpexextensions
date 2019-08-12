package com.kc2hiz.lpexextensions;

import com.ibm.lpex.core.LpexView;
import com.ibm.lpex.core.LpexAction;
import com.ibm.lpex.core.LpexViewAdapter;

public class UserProfile {

    public static void userProfile(LpexView lpexView) {

        // semicolon at EOL and comment
        lpexView.defineAction("insertSemicolon2Action", new LpexAction() {

            public void doAction(LpexView view) {
                // go to the end of the line
                view.doAction(view.actionId("contextEnd"));
                // insert a semicolon and comment string
                view.doCommand("insertText ; /*  */");
                // position the cursor in the middle of the comment
                view.doAction(view.actionId("left"));
                view.doAction(view.actionId("left"));
                view.doAction(view.actionId("left"));
            }

            public boolean available(LpexView view) {
                // allow the action to run for any visible text line in a
                // writable document
                return view.currentElement() != 0 && !view.queryOn("readonly");
            }
        });

        
        // register a listener for when the view is shown
        // https://www.ibm.com/support/knowledgecenter/SSAE4W_9.6.0/com.ibm.lpex.doc.isv/api/com/ibm/lpex/core/LpexViewAdapter.html
        lpexView.addLpexViewListener(new LpexViewAdapter() {
            // called after the updateProfile command has completed
            public void shown(LpexView view) {
                handleShown(view);
            }
        });

        // Assign keys "Ctrl+5" to run insertSemicolon2Action
        lpexView.doCommand("set keyAction.c-5 insertSemicolon2Action");

        // force insert mode
        // doesn't seem to work; maybe preload() is too early?
//        lpexView.doDefaultCommand("set insertMode on");

    }

    // runs when the view is finished being displayed
    // called by LpexViewListener
    protected static void handleShown(LpexView view) {
        // force insert mode
        view.doDefaultCommand("set insertMode on");
    }

}