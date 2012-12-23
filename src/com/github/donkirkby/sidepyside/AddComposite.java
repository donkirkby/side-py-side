package com.github.donkirkby.sidepyside;

import java.util.ListResourceBundle;

import org.eclipse.compare.Splitter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener4;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;

/**
 * This plug in wraps the main PyEdit in an extra splitter and sets it up
 * like the compare editor. The only analysis it does is to display line length,
 * but that could be changed to whatever kind of analysis you like.
 * @author don
 *
 */
public class AddComposite implements IPyEditListener, IPyEditListener4 {
	private ISourceViewer mainViewer;
	private IDocument mainDocument;
	private Document displayDocument;
	private SourceViewer displayViewer;
	
	/**
	 * Wire up a new editor so that it will be displayed the way we want.
	 */
	@Override
	public void onEditorCreated(PyEdit edit) {
		edit.onCreatePartControl.registerListener(
				new ICallbackListener<Composite>() {
			
			/**
			 * This callback inserts a new composite inside the standard window
			 * and then returns the left pane of the splitter as the new parent
			 * for the main editor controls.
			 * @param parent The standard window that usually holds the editor.
			 * @return The new control that the editor can be created in.
			 */
			public Object call(Composite parent) {
				Splitter splitter = new Splitter(parent, SWT.HORIZONTAL);
				
				Composite editorContent = new Composite(splitter, SWT.NONE);
				editorContent.setLayout(new FillLayout());
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				editorContent.setLayoutData(gridData);
				
				Composite liveDisplay = new Composite(splitter, SWT.NONE);
				liveDisplay.setLayout(new FillLayout());
				GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
				liveDisplay.setLayoutData(gridData2);
				
				VerticalRuler ruler = new VerticalRuler(12);
				int styles = 
						SWT.V_SCROLL | 
						SWT.H_SCROLL | 
						SWT.MULTI | 
						SWT.BORDER | 
						SWT.FULL_SELECTION;
				displayViewer = 
						new SourceViewer(liveDisplay, ruler, styles);
				SourceViewerConfiguration config = 
						new SourceViewerConfiguration();
				displayViewer.configure(config);
				displayDocument = new Document("");
				displayViewer.setDocument(displayDocument);
				
				displayViewer.addViewportListener(new IViewportListener() {
					
					/**
					 * Update the scroll bar of the main viewer when the
					 * display viewer is scrolled.
					 */
					@Override
					public void viewportChanged(int verticalOffset) {
						if (mainViewer != null) {
							mainViewer.getTextWidget().setTopPixel(
									verticalOffset);
						}
					}
				});
				new TextViewerSupport(displayViewer); // registers itself
				
			    splitter.setVisible(editorContent, true);
			    splitter.setVisible(liveDisplay, true);

				return editorContent;
			}
		});
		edit.onAfterCreatePartControl.registerListener(
				new ICallbackListener<ISourceViewer>() {
			
			/**
			 * Copy the style settings from the main viewer to the display
			 * viewer.
			 * @param newViewer The main viewer that was just created.
			 * @return The main viewer.
			 */
			@Override
			public Object call(ISourceViewer newViewer) {
				mainViewer = newViewer;
				displayViewer.getTextWidget().setFont(
						mainViewer.getTextWidget().getFont());
				return newViewer;
			}
		});
		edit.onCreateSourceViewer.registerListener(
				new ICallbackListener<PySourceViewer>() {

			/**
			 * Wire up the main viewer after it's created.
			 * @param viewer The main viewer that was just created.
			 * @return The main viewer.
			 */
			@Override
			public Object call(PySourceViewer newViewer) {

				newViewer.addViewportListener(new IViewportListener() {
					
					/**
					 * Update the scroll bar of the display viewer when the main
					 * viewer is scrolled.
					 * @param viewer The main viewer.
					 * @return
					 */
					@Override
					public void viewportChanged(int verticalOffset) {
						if (displayViewer != null) {
							displayViewer.getTextWidget().setTopPixel(
									verticalOffset);
						}
					}
				});
				return newViewer;
			}
		});
	}

	@Override
	public void onSave(PyEdit edit, IProgressMonitor monitor) {
	}

	@Override
	public void onCreateActions(
			ListResourceBundle resources, 
			PyEdit edit,
			IProgressMonitor monitor) {
	}

	@Override
	public void onDispose(PyEdit edit, IProgressMonitor monitor) {
	}

	/**
	 * Wire up the main document and perform the first analysis.
	 */
	@Override
	public void onSetDocument(
			IDocument document, 
			PyEdit edit,
			IProgressMonitor monitor) {
		mainDocument = document;
		document.addDocumentListener(new IDocumentListener() {

			/**
			 * Analyse the document and display the results whenever the
			 * document changes.
			 */
			@Override
			public void documentChanged(DocumentEvent event) {
				analyseDocument(event.getDocument());
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
		
		// Perform the first analysis, but it has to run on the display thread.
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	analyseDocument(mainDocument);
		    }
		});
	}

	/**
	 * Analyse the document and display the results. This sample analysis is
	 * trivial and should be replaced with something useful.
	 * @param document
	 */
	private void analyseDocument(IDocument document) {
		StringBuilder builder = new StringBuilder();
		String text = document.get();
		String[] lines = text.split("\n");
		for (String line : lines) {
			for (int i = 0; i < line.length() - 1; i++) {
				builder.append(' ');
			}
			if (line.length() > 0)
			{
				builder.append("X");
			}
			builder.append("\n");
		}
		builder.append("\n");
		displayDocument.set(builder.toString());
		
		// Update the scroll position after changing the text.
		displayViewer.getTextWidget().setTopPixel(
				mainViewer.getTextWidget().getTopPixel());
	}
}
