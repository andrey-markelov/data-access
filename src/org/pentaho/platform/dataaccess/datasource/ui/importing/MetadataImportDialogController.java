/*!
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
*/

package org.pentaho.platform.dataaccess.datasource.ui.importing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.platform.dataaccess.datasource.wizard.DatasourceMessages;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulConfirmBox;
import org.pentaho.ui.xul.components.XulLabel;
import org.pentaho.ui.xul.components.XulMessageBox;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.containers.XulVbox;
import org.pentaho.ui.xul.gwt.tags.GwtConfirmBox;
import org.pentaho.ui.xul.gwt.tags.GwtMessageBox;
import org.pentaho.ui.xul.stereotype.Bindable;
import org.pentaho.ui.xul.util.AbstractXulDialogController;
import org.pentaho.ui.xul.util.XulDialogCallback;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MetadataImportDialogController extends AbstractXulDialogController<MetadataImportDialogModel>
  implements IImportPerspective, IOverwritableController {
  /**
   *
   */
  private static final String UPLOAD_URL = "plugin/data-access/api/datasource/metadata/uploadxmi";//POST
  private static final String METADATA_IMPORT_XMI_URL = "plugin/data-access/api/datasource/metadata/import/uploaded"; //POST
  private static final String METADATA_IMPORT_DSW_URL = "plugin/data-access/api/datasource/dsw/import/uploaded"; //POST
  private static final String METADATA_CHECK_URL = "plugin/data-access/api/datasource/metadata/iscontainsmodel"; //GET
  private static Integer FILE_UPLOAD_SUFFIX = 0;
  private BindingFactory bf;
  private XulButton acceptButton;
  private XulTree localizedBundlesTree;
  private XulTextbox domainIdText;
  private XulLabel metaFileLocation;
  private XulDialog importDialog;
  private ResourceBundle resBundle;
  private MetadataImportDialogModel importDialogModel;
  private XulLabel fileLabel;
  private FlowPanel mainFormPanel;
  private FlowPanel propertiesFileImportPanel;
  private XulVbox hiddenArea;
  private DatasourceMessages messages = null;
  private boolean overwrite;
  private static FormPanel.SubmitCompleteHandler submitHandler = null;

  // GWT controls
  private FormPanel formPanel;
  private FileUpload metadataFileUpload;
  private TextBox formDomainIdText;

  protected static final int OVERWRITE_EXISTING_SCHEMA = 8;

  public void init() {
    try {
      resBundle = (ResourceBundle) super.getXulDomContainer().getResourceBundles().get( 0 );
      importDialogModel = new MetadataImportDialogModel();
      localizedBundlesTree = (XulTree) document.getElementById( "localizedBundlesTree" );
      domainIdText = (XulTextbox) document.getElementById( "domainIdText" );
      domainIdText.addPropertyChangeListener( new DomainIdChangeListener() );
      importDialog = (XulDialog) document.getElementById( "importDialog" );
      fileLabel = (XulLabel) document.getElementById( "fileLabel" );
      metaFileLocation = (XulLabel) document.getElementById( "uploadFileLabel" );
      acceptButton = (XulButton) document.getElementById( "importDialog_accept" );
      hiddenArea = (XulVbox) document.getElementById( "metadataImportCard" );
      acceptButton.setDisabled( true );

      bf.setBindingType( Binding.Type.ONE_WAY );
      Binding localizedBundlesBinding =
        bf.createBinding( importDialogModel, "localizedBundles", localizedBundlesTree, "elements" );
      localizedBundlesBinding.fireSourceChanged();
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

  private void createWorkingForm() {
    if ( formPanel == null ) {
      formPanel = new FormPanel();
      formPanel.setMethod( FormPanel.METHOD_POST );
      formPanel.setEncoding( FormPanel.ENCODING_MULTIPART );
      formPanel.setAction( UPLOAD_URL );
      formPanel.getElement().getStyle().setProperty( "position", "absolute" );
      formPanel.getElement().getStyle().setProperty( "visibility", "hidden" );
      formPanel.getElement().getStyle().setProperty( "overflow", "hidden" );
      formPanel.getElement().getStyle().setProperty( "clip", "rect(0px,0px,0px,0px)" );
      mainFormPanel = new FlowPanel();
      formPanel.add( mainFormPanel );
      propertiesFileImportPanel = new FlowPanel();
      mainFormPanel.add( propertiesFileImportPanel );

      formDomainIdText = new TextBox();
      formDomainIdText.setName( "domainId" );
      mainFormPanel.add( formDomainIdText );
      metadataFileUpload = new FileUpload();
      metadataFileUpload.setName( "metadataFile" );
      metadataFileUpload.getElement().setId( "metaFileUpload" );
      metadataFileUpload.getElement().appendChild( ( new Hidden( "mark_temporary", Boolean.TRUE.toString() ).getElement() ));
      mainFormPanel.add( metadataFileUpload );
      metadataFileUpload.addChangeHandler( new ChangeHandler() {
        @Override
        public void onChange( ChangeEvent event ) {
          String filename = ( (FileUpload) event.getSource() ).getFilename();
          if ( filename != null && filename.trim().length() > 0 ) {
            metaFileLocation.setValue( filename );
            importDialogModel.setUploadedFile( filename );
            acceptButton.setDisabled( !isValid() );
          } else {
            metaFileLocation
              .setValue( resBundle.getString( "importDialog.SELECT_METAFILE_LABEL", "browse for metadata file" ) );
            importDialogModel.setUploadedFile( null );
            acceptButton.setDisabled( !isValid() );
          }
        }
      } );
      
      mainFormPanel.add( metadataFileUpload );
      formPanel.addSubmitCompleteHandler( createSubmitCompleteHandler() );
      
      VerticalPanel vp = (VerticalPanel) hiddenArea.getManagedObject();
      vp.add( formPanel );
    }
  }
  
  private SubmitCompleteHandler createSubmitCompleteHandler() {
    return new SubmitCompleteHandler() {
      
      @Override
      public void onSubmitComplete( SubmitCompleteEvent event ) {
        
        final String jsonResponseText = new HTML(event.getResults()).getText();//delete all surrounded tags
        final JSONObject jsonResponse;
        JSONValue jsonVal = JSONParser.parseStrict( jsonResponseText );
        
        if( jsonVal!= null ) {
          jsonResponse = jsonVal.isObject();
        } else {
          jsonResponse = null;
        }
        if( jsonResponse == null ) {
          raiseImportError( "wrong data from xmi file checker" ); 
          return;
        }
        
        String tempFileName = jsonResponse.get( "xmiFileName" ).isString().stringValue();
        RequestBuilder checkFileRequest = new RequestBuilder( RequestBuilder.GET , 
            METADATA_CHECK_URL + "?tempFileName=" + URL.encode( tempFileName ) );

        checkFileRequest.setCallback( new RequestCallback() {
          @Override
          public void onResponseReceived( Request request, Response response ) {
            if( response.getStatusCode() == 200 ) {
              if( Boolean.TRUE.toString().equalsIgnoreCase( response.getText() ) ) {
                confirm( resBundle.getString( "importDialog.IMPORT_METADATA" ), 
                    resBundle.getString( "importDialog.CONFIRMATION_LOAD_DSW" ), 
                    "Yes", "No", 
                    new AsyncCallback<Boolean>() {
                      @Override
                      public void onSuccess( Boolean result ) {
                        new XmiImporterRequest().doImport( (Boolean)result?METADATA_IMPORT_DSW_URL:METADATA_IMPORT_XMI_URL, jsonResponse );
                      }
                      
                      @Override
                      public void onFailure( Throwable caught ) {
                        Window.alert( caught.getMessage() );
                      }
                    });
              } else if ( !Boolean.FALSE.toString().equals( response.getText() ) ) {
                raiseImportError( "wrong data from xmi file checker" );   
              }

            } else {
              raiseImportError( "[server data error] , wrong code: " + response.getStatusCode() );
            }
          }
          
          @Override
          public void onError( Request request, Throwable exception ) {
            raiseImportError( "[request error] " + exception.getMessage() );
          }
        } );
        try {
          checkFileRequest.send();
        } catch ( RequestException e ) {
          raiseImportError( e.getMessage() );
        }
        
      }
    };
  }
  
  private class XmiImporterRequest implements RequestCallback {
    
    public void doImport( String url, JSONObject jsonResponse ) {
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.POST , url );
      requestBuilder.setRequestData( "domainId=" + URL.encode( importDialogModel.getDomainId() ) +
          "&jsonFileList=" + URL.encode( jsonResponse.toString() ) +
          "&overwrite=true");
      
      requestBuilder.setHeader( "Content-Type", "application/x-www-form-urlencoded" );
      
      requestBuilder.setCallback( this );
      try {
        requestBuilder.send();
      } catch ( RequestException e ) {
        raiseImportError( e.getMessage() );
      }
    }
    
    @Override
    public void onResponseReceived( Request request, Response response ) {
      if( response.getStatusCode() == 200 ) {
        showMessagebox( resBundle.getString( "importDialog.IMPORT_METADATA" ), resBundle.getString( "importDialog.SUCCESS_METADATA_IMPORT" ) );
      } else {
        raiseImportError( "[server data error] , wrong code: " + response.getStatusCode() );
      }
    }
    
    @Override
    public void onError( Request request, Throwable exception ) {
      raiseImportError( "[request error] " + exception.getMessage() );
    }
    
  }

  public XulDialog getDialog() {
    return importDialog;
  }

  public MetadataImportDialogModel getDialogResult() {
    return importDialogModel;
  }

  public boolean isValid() {
    return importDialogModel.isValid();
  }

  @Bindable
  public void setMetadataFile() {
    jsClickUpload( metadataFileUpload.getElement().getId() );
  }

  @Bindable
  public void removeLocalizedBundle() {
    int[] selectedRows = localizedBundlesTree.getSelectedRows();
    if ( selectedRows.length == 1 ) {
      propertiesFileImportPanel.remove( selectedRows[ 0 ] );
      importDialogModel.removeLocalizedBundle( selectedRows[ 0 ] );
    }
  }

  @Bindable
  public void addLocalizedBundle() {
    final FileUpload localizedBundleUpload = new FileUpload();
    localizedBundleUpload.setName( "localeFiles" );
    localizedBundleUpload.getElement().setId( "propertyFileUpload" + FILE_UPLOAD_SUFFIX++ );
    localizedBundleUpload.addChangeHandler( new ChangeHandler() {
      @Override
      public void onChange( ChangeEvent event ) {
        String fileName = ( (FileUpload) event.getSource() ).getFilename();
        if ( fileName == null || fileName.length() < 1 ) {  // Trying to detect a cancel
          propertiesFileImportPanel.remove( localizedBundleUpload );
        } else {
          importDialogModel.addLocalizedBundle( fileName, fileName );
        }
      }
    } );
    propertiesFileImportPanel.add( localizedBundleUpload );
    jsClickUpload( localizedBundleUpload.getElement().getId() );
  }

  native void jsClickUpload( String uploadElement ) /*-{
    $doc.getElementById(uploadElement).click();
  }-*/;

  private void reset() {
    metaFileLocation
      .setValue( resBundle.getString( "importDialog.SELECT_METAFILE_LABEL", "browse for metadata file" ) );
    importDialogModel.removeAllLocalizedBundles();
    importDialogModel.setUploadedFile( null );
    if ( formPanel != null && RootPanel.get().getWidgetIndex( formPanel ) != -1 ) {
      RootPanel.get().remove( formPanel );
    }
    acceptButton.setDisabled( true );
    domainIdText.setValue( "" );
    overwrite = false;
    formPanel = null;

    removeHiddenPanels();
  }


  public void concreteUploadCallback( String fileName, String uploadedFile ) {
    importDialogModel.addLocalizedBundle( fileName, uploadedFile );
  }

  public void genericUploadCallback( String uploadedFile ) {
    importDialogModel.setUploadedFile( uploadedFile );
    acceptButton.setDisabled( !isValid() );
  }
  
  private void raiseImportError( String message ) {
    showMessagebox( resBundle.getString( "importDialog.IMPORT_METADATA" ), 
        resBundle.getString( "importDialog.ERROR_IMPORTING_METADATA" ) + ": " + message );
    hideDialog();
  }

  public void showDialog() {
    reset();
    importDialog.setTitle( resBundle.getString( "importDialog.IMPORT_METADATA", "Import Metadata" ) );
    fileLabel.setValue( resBundle.getString( "importDialog.XMI_FILE", "XMI File" ) + ":" );
    super.showDialog();
    createWorkingForm();
  }

  public void setBindingFactory( final BindingFactory bf ) {
    this.bf = bf;
  }

  public String getName() {
    return "metadataImportDialogController";
  }

  public FormPanel getFormPanel() {
    return formPanel;
  }

  class DomainIdChangeListener implements PropertyChangeListener {
    public void propertyChange( PropertyChangeEvent evt ) {
      formDomainIdText.setText( evt.getNewValue().toString() );
      importDialogModel.setDomainId( evt.getNewValue().toString() );
      acceptButton.setDisabled( !isValid() );
    }
  }

  public void buildAndSetParameters() {

    Hidden overwriteParam = new Hidden( "overwrite", String.valueOf( overwrite ) );
    mainFormPanel.add( overwriteParam );

  }

  public void removeHiddenPanels() {
    // Remove all previous hidden form parameters otherwise parameters
    // from a previous import would get included in current form submit
    for ( int i = 0; mainFormPanel != null && i < mainFormPanel.getWidgetCount(); i++ ) {
      if ( mainFormPanel.getWidget( i ).getClass().equals( Hidden.class ) ) {
        mainFormPanel.remove( mainFormPanel.getWidget( i ) );
      }
    }
  }

  /**
   * Convert to $NLS$
   *
   * @param results
   * @return msg int PUBLISH_TO_SERVER_FAILED = 1; int PUBLISH_GENERAL_ERROR = 2; int PUBLISH_DATASOURCE_ERROR = 6; int
   * PUBLISH_USERNAME_PASSWORD_FAIL = 5; int PUBLISH_XMLA_CATALOG_EXISTS = 7; int PUBLISH_SCHEMA_EXISTS_ERROR = 8;
   */
  public String convertToNLSMessage( String results, String fileName ) {
    String msg = results;
    int code = new Integer( results ).intValue();
    String messageId;
    String[] parameters = new String[ 0 ];
    switch( code ) {
      case 1: //PUBLISH_TO_SERVER_FAILED = 1;
        messageId = "Metadata.PUBLISH_TO_SERVER_FAILED";
        break;
      case 2: //PUBLISH_GENERAL_ERROR = 2;
        messageId = "Metadata.PUBLISH_GENERAL_ERROR";
        break;
      case 3: //PUBLISH_DATASOURCE_ERROR = 6;
        messageId = "Metadata.PUBLISH_DATASOURCE_ERROR";
        break;
      case 4: //PUBLISH_USERNAME_PASSWORD_FAIL = 5;
        messageId = "Metadata.PUBLISH_USERNAME_PASSWORD_FAIL";
        break;
      case 7: //PUBLISH_XMLA_CATALOG_EXISTS = 7;
        messageId = "Metadata.PUBLISH_XMLA_CATALOG_EXISTS";
        break;
      case 8: //PUBLISH_SCHEMA_EXISTS_ERROR
        messageId = "Metadata.OVERWRITE_EXISTING_SCHEMA";
        break;
      case 10: //PUBLISH_PROHIBITED_SYMBOLS_ERROR
        messageId = "Metadata.PUBLISH_PROHIBITED_SYMBOLS_ERROR";
        parameters = new String[] { NameUtils.reservedCharListForDisplay( ", " ) };
        break;
      default:
        messageId = "Metadata.ERROR";
        break;
    }
    msg = messages.getString( messageId, parameters );
    return msg + " Metadata File: " + fileName;
  }


//  public void handleFormPanelEvent( FormPanel.SubmitCompleteEvent event ) {
//    if ( event.getResults().contains( "SUCCESS" ) || event.getResults().contains( "3" ) ) {
//      showMessagebox( messages.getString( "Metadata.SUCCESS" ),
//        "Metadata File " + importDialogModel.getUploadedFile() + " has been uploaded" );
//    } else {
//      String message = event.getResults();
//      //message = message.substring(4, message.length() - 6);
//      if ( message != null && !"".equals( message ) && message.length() == 1 ) {
//        int code = new Integer( message ).intValue();
//        if ( code == OVERWRITE_EXISTING_SCHEMA && !overwrite ) { //Existing FIle Dialog
//          overwriteFileDialog();
//        } else {
//          showMessagebox( messages.getString( "Metadata.ERROR" ),
//            convertToNLSMessage( event.getResults(), importDialogModel.getUploadedFile() ) );
//        }
//      } else {
//        showMessagebox( messages.getString( "Metadata.SERVER_ERROR" ),
//          convertToNLSMessage( event.getResults(), importDialogModel.getUploadedFile() ) );
//      }
//    }
//  }
//
//  @Bindable
//  public void overwriteFileDialog() {
//    //Experiment
//    XulConfirmBox confirm = null;
//    try {
//      confirm = (XulConfirmBox) document.createElement( "confirmbox" );
//    } catch ( XulException e ) {
//      Window.alert( e.getMessage() );
//    }
//    confirm.setTitle( "Confirmation" );
//    confirm.setMessage( messages.getString( "Metadata.OVERWRITE_EXISTING_SCHEMA" ) );
//    confirm.setAcceptLabel( "Ok" );
//    confirm.setCancelLabel( "Cancel" );
//    confirm.addDialogCallback( new XulDialogCallback<String>() {
//      public void onClose( XulComponent component, Status status, String value ) {
//        if ( status == XulDialogCallback.Status.ACCEPT ) {
//          overwrite = true;
//          removeHiddenPanels();
//          buildAndSetParameters();
//          formPanel.submit();
//        }
//      }
//
//      public void onError( XulComponent component, Throwable err ) {
//        return;
//      }
//    } );
//    confirm.open();
//  }

  /**
   * Shows a informational dialog
   *
   * @param title   title of dialog
   * @param message message within dialog
   */
  private void showMessagebox( final String title, final String message ) {
    XulMessageBox messagebox = new GwtMessageBox();

    messagebox.setTitle( title );
    messagebox.setMessage( message );
    messagebox.open();
  }
  
  /**
   * Shows a confirmation dialog
   * 
   * @param title
   * @param message
   * @param okButtonLabel
   * @param cancelButtonLabel
   * @param onResulthandler
   */
  private void confirm( final String title, final String message, final String okButtonLabel, final String cancelButtonLabel, 
      final AsyncCallback<Boolean> onResulthandler ) {
    XulConfirmBox confirm = new GwtConfirmBox();
    confirm.setTitle( title );
    confirm.setMessage( message );
    confirm.setAcceptLabel( okButtonLabel );
    confirm.setCancelLabel( cancelButtonLabel );
    confirm.addDialogCallback( new XulDialogCallback<String>() {
      public void onClose( XulComponent component, Status status, String value ) {
        if( onResulthandler!= null ) {
          onResulthandler.onSuccess( status == XulDialogCallback.Status.ACCEPT );
        }
        
      }
      public void onError( XulComponent component, Throwable err ) {
        onResulthandler.onFailure( err );
        return;
      }
    } );
    confirm.open();
  }

  /**
   * pass localized messages from Entry point initialization
   *
   * @param datasourceMessages
   */
  public void setDatasourceMessages( DatasourceMessages datasourceMessages ) {
    this.messages = datasourceMessages;
  }

  /**
   * helper method for dialog display
   *
   * @return
   */
  public String getFileName() {
    return this.importDialogModel.getUploadedFile();
  }

  public void setOverwrite( boolean overwrite ) {
    this.overwrite = overwrite;

  }

}
