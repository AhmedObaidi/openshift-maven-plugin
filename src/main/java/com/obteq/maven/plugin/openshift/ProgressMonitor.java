/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.obteq.maven.plugin.openshift;

/**
 *
 * @author ahmed
 */
public interface ProgressMonitor {

    public void progress(long fileSize, long sentBytes, long speed);
}
