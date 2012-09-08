package com.github.donkirkby.sidepyside;

import java.util.ListResourceBundle;

import org.eclipse.compare.Splitter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener4;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;

public class AddComposite implements IPyEditListener, IPyEditListener4 {
	private Document displayDocument;
	private SourceViewer displayViewer;
	
	@Override
	public void onEditorCreated(PyEdit edit) {
		edit.onCreatePartControl.registerListener(new ICallbackListener<Composite>() {
			
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
				
			    splitter.setVisible(editorContent, true);
			    splitter.setVisible(liveDisplay, true);

				return editorContent;
			}
		});
		edit.onCreateSourceViewer.registerListener(
				new ICallbackListener<PySourceViewer>() {
			
			@Override
			public Object call(PySourceViewer viewer) {
				viewer.addViewportListener(new IViewportListener() {
					
					@Override
					public void viewportChanged(int verticalOffset) {
						if (displayViewer != null) {
							displayViewer.getTextWidget().setTopPixel(
									verticalOffset);
						}
					}
				});
				return viewer;
			}
		});
	}

	@Override
	public void onSave(PyEdit edit, IProgressMonitor monitor) {
	}

	@Override
	public void onCreateActions(ListResourceBundle resources, PyEdit edit,
			IProgressMonitor monitor) {
	}

	@Override
	public void onDispose(PyEdit edit, IProgressMonitor monitor) {
	}

	@Override
	public void onSetDocument(
			IDocument document, 
			PyEdit edit,
			IProgressMonitor monitor) {
		
		document.addDocumentListener(new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				StringBuilder builder = new StringBuilder();
				String text = event.getDocument().get();
				String[] lines = text.split("\n");
				for (String line : lines) {
					for (int i = 0; i < line.length(); i++) {
						builder.append(' ');
					}
					builder.append("X\n");
				}
				displayDocument.set(builder.toString());
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}
}
