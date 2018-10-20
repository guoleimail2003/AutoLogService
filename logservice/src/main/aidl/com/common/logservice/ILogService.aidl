
package com.common.logservice;

import android.os.Bundle;

interface ILogService
{
    void uploadUserException(String description, in Bundle info);

    void uploadLogFile(String description, String path, in Bundle info);
	
    void validate(String description, String code, in Bundle info);
	
    void download(String description, String url, String path, in Bundle info);
	
    void checkUpdate(String description, in Bundle info);
	
    void checkCategory(String description, in Bundle info);
}

