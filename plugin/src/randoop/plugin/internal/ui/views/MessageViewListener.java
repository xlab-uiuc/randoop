package randoop.plugin.internal.ui.views;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.PlatformUI;

import randoop.plugin.RandoopPlugin;
import randoop.plugin.internal.core.runtime.IMessageListener;
import randoop.runtime.CreatedJUnitFile;
import randoop.runtime.ErrorRevealed;
import randoop.runtime.IMessage;
import randoop.runtime.PercentDone;
import randoop.runtime.RandoopStarted;

public class MessageViewListener implements IMessageListener {
  private TestGeneratorViewPart fViewPart;

  public MessageViewListener(TestGeneratorViewPart viewPart) {
    fViewPart = viewPart;
  }

  @Override
  public void handleMessage(IMessage m) {
    if (m instanceof RandoopStarted) {
      PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
          @Override
          public void run() {
            fViewPart.getProgressBar().start();
            fViewPart.getCounterPanel().reset();
            fViewPart.randoopErrors.reset();
            
          }
        });
    } else if (m instanceof PercentDone) {
      final PercentDone p = (PercentDone)m;
      PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          fViewPart.getProgressBar().setPercentDone(p.getPercentDone());
          fViewPart.getCounterPanel().numSequences(p.getSequencesGenerated());
        }
      });
    } else if (m instanceof ErrorRevealed) {
      final ErrorRevealed err = (ErrorRevealed)m;
      PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          fViewPart.getProgressBar().error();
          fViewPart.getCounterPanel().errors();
          fViewPart.randoopErrors.add(err);          
        }
      });
    } else if (m instanceof CreatedJUnitFile) {
      final CreatedJUnitFile fileCreated = (CreatedJUnitFile) m;
      PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          try {
            File f = fileCreated.getFile();

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            System.out.println("@@@" + f.toString());
            IResource driver = root.findMember(new Path(f.toString()));
            
            // TODO root may be null. If it is, notify user that the given
            // file was not found.

            IProject project = driver.getProject();
            IJavaProject jproject = (IJavaProject) project
                .getNature(JavaCore.NATURE_ID);
            IJavaElement element = JavaCore.create(driver, jproject);

            Assert.isTrue(element instanceof ICompilationUnit);
            element.getCorrespondingResource().refreshLocal(
                IResource.DEPTH_ONE, null);

            if (fileCreated.isDriver()) {
              fViewPart.setDriver((ICompilationUnit) element);
            }
          } catch (CoreException e) {
            RandoopPlugin.log(e);
          }
        }
      });
    }
  }

  @Override
  public void handleTermination() {
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        fViewPart.getProgressBar().stop();
      }
    });
  }
}
