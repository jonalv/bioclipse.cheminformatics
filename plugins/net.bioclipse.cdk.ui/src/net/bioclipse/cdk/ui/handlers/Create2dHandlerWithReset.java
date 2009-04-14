/*******************************************************************************
 * Copyright (c) 2008 The Bioclipse Project and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Stefan Kuhn
 ******************************************************************************/
package net.bioclipse.cdk.ui.handlers;

import net.bioclipse.cdk.business.Activator;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.core.util.LogUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.openscience.cdk.interfaces.IAtom;

/**
 * A handler class for a Generate 2D Coordinates menu item
 */
public class Create2dHandlerWithReset extends AbstractHandler {

    private static final Logger logger =
                                               Logger
                                                       .getLogger( Create2dHandlerWithReset.class );
    public int                  answer;

    public Object execute( ExecutionEvent event ) throws ExecutionException {
        doCreation(true,false);
        return null;
    }
    
    /**
     * This method creates the coordinates and saves the file on the selection.
     * 
     * @param withReset If true, the other set of coordinates is set to null. This should be used on mol files, since these can only hold one set (3d or 2d).
     * @param make3D true = 3d is generated, false = 2d is generated.
     */
    public static void doCreation(boolean withReset, boolean make3D){
        ISelection sel =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getSelectionService().getSelection();
      if ( !sel.isEmpty() ) {
          if ( sel instanceof IStructuredSelection ) {
              IStructuredSelection ssel = (IStructuredSelection) sel;
              for(int i=0;i<ssel.toArray().length;i++){
                ICDKMolecule mol;
                try {
                    mol =
                            Activator.getDefault().getCDKManager()
                                    .loadMolecule(
                                                   (IFile) ssel.toArray()[i], new NullProgressMonitor() );
                    if(make3D){
                        mol =
                            (ICDKMolecule) Activator.getDefault()
                                    .getCDKManager()
                                    .generate3dCoordinates( new IMolecule[]{mol} )[0] ;                    
                    }else{
                      mol =
                              (ICDKMolecule) Activator.getDefault()
                                      .getCDKManager()
                                      .generate2dCoordinates( new IMolecule[]{mol} )[0];
                    }
                    if(withReset){
                      //we set the other coordinates to null, since when writing out, they might override
                      for(IAtom atom : mol.getAtomContainer().atoms()){
                          if(make3D)
                              atom.setPoint2d( null );
                          else
                              atom.setPoint3d( null );
                      }
                    }
                } catch ( Exception e ) {
                    LogUtils.handleException( e, logger );
                    return;
                }
                MessageBox mb =
                        new MessageBox( new Shell(), SWT.YES | SWT.NO | SWT.CANCEL
                                                     | SWT.ICON_QUESTION );
                mb.setText( "Change file: "+((IFile) ssel.toArray()[i]).getName() );
                mb.setMessage( "Do you want to write the "+ (make3D ? "3D" : "2D")+ " coordinates into the existing file? If no, a new one will be created." );
                int val = mb.open();
                if ( val == SWT.YES ) {
                    try {
                        Activator.getDefault().getCDKManager()
                                .saveMolecule(
                                               mol,
                                               (IFile) ssel.toArray()[i],
                                               true);
                    } catch ( Exception e ) {
                        throw new RuntimeException( e.getMessage() );
                    }
                } else if ( val == SWT.NO ){
                    SaveAsDialog dialog = new SaveAsDialog( new Shell() );
                    dialog.setOriginalFile( (IFile) ssel.toArray()[i] );
                    int saveasreturn = dialog.open();
                    IPath result = dialog.getResult();
                    if ( saveasreturn != SaveAsDialog.CANCEL ) {
                        if ( dialog.getResult().getFileExtension() == null )
                            result =
                                    result.addFileExtension( ((IFile) ssel.toArray()[i])
                                            .getFileExtension() );
                        try {
                            Activator
                                    .getDefault()
                                    .getCDKManager()
                                    .saveMolecule(
                                                   mol,
                                                   ((IFile) ssel.toArray()[i])
                                                           .getWorkspace()
                                                           .getRoot()
                                                           .getFile(
                                                                     result ),
                                                   true );
                        } catch ( Exception e ) {
                            throw new RuntimeException( e.getMessage() );
                        }
                    }
                  }
              }
          }
      }
    }
}